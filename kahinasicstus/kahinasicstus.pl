:- module(kahinasicstus,[end_trace_session/0,
                         add_kbreakpoint/3,
                         abort_hook/2,
                         breakpoint_action_hook/5,
                         post_step_hook/5,
                         pre_exit_hook/5,
                         get_jvm/1,
                         get_kahina_instance/2,
                         get_bridge/3]).

:- use_module(library(charsio)).
:- use_module(library(lists)).
:- use_module(library(jasper)).
:- use_module(library(system)).
:- use_module(library(terms)).

% ------------------------------------------------------------------------------
% BREAKPOINT EXPANSION
% The interface between SICStus Prolog's tracer and kahinasicstus. Defines the
% breakpoint action kahina_breakpoint_action/1.
% ------------------------------------------------------------------------------

:- multifile user:breakpoint_expansion/2.

% Valid Options:
%   autoskip(Autoskip) - If this breakpoint matches at a call or redo port, the
%       called/redone step will be skipped automatically if Autoskip is true.
%       Default is false.
%   layer(Layer) - If this option is given and Layer is an integer, it will be
%       registered with Kahina as the layer of this step at call ports. Note
%       that Kahina may ignore this, depending on the layer decider used.
%   source_code_location(Callback,File,Line) - Can be given to specify custom
%       source code locations for goals matching the breakpoint. On a match,
%       the goal Callback will be called. If it succeeds, File is used as the
%       absolute path of the corresponding source file and Line is used as the
%       corresponding line number in that file.
%   goal_desc(Callback,Module,Goal,Desc) - Can be given to specify custom
%       mappings from modules/goals to goal descriptions (used as node labels).
%       On a match, Module and Goal will be bound to the values for the current
%       match, then Callback will be called. If it succeeds, Desc is used as the
%       goal description. Otherwise, a default is used (currently: the goal
%       itself with the module prefix stripped away).
user:breakpoint_expansion(kahina_breakpoint_action(Options),[
    % The three action variables Show, Mode, Command control Prolog's behavior
    % on encountering a breakpoint. We use show/1, mode/1, command/1 terms to
    % set their values. We always set Show to silent (no output on console). The
    % values for Mode and Command depend on the response from the GUI. For lists
    % of possible values for each action variable, see section 7.9.9 of the SP3
    % manual.
    show(silent),
    inv(Inv),
    port(Port),
    true(kahinasicstus:kahina_breakpoint_action(Inv,Port,Mode,Command,Options)),
    mode(Mode),
    command(Command)]).

% ------------------------------------------------------------------------------
% HOOKS
% Predicates whose behavior can be influenced using hooks, and corresponding
% multifile declarations.
% ------------------------------------------------------------------------------

:- multifile breakpoint_action_hook/5.

% kahina_breakpoint_action(+Inv,+Port,-Mode,-Command,+Options)
kahina_breakpoint_action(Inv,Port,Mode,Command,Options) :-
  breakpoint_action_hook(Port,Inv,Mode,Command,Options),
  !.
kahina_breakpoint_action(Inv,Port,Mode,Command,Options) :-
  get_bridge(Inv,Port,Bridge),
  get_jvm(JVM),
  act(Port,Inv,Bridge,JVM,Options),
  get_action(Port,Bridge,JVM,Action),
  (memberchkid(autoskip(true),Options)
  -> Autoskip = true
   ; Autoskip = false),
  action_mode_command(Action,Mode,Command,Inv,Port,Autoskip).

:- multifile abort_hook/2.

% action_mode_command(+ActionCode,-Mode,-Command,+Inv,+Port,+Autoskip)
action_mode_command(115,skip(Inv),proceed,Inv,_Port,_Autoskip) :- % s(kip)
  !.
action_mode_command(102,debug,proceed,_Inv,fail,_Autoskip) :-     % f(ail) at fail ports TODO what about exception ports?
  !.
action_mode_command(102,debug,fail(Inv),Inv,_Port,_Autoskip) :-   % f(ail) at other ports
  !.
action_mode_command(97,Mode,Command,_Inv,_Port,_Autoskip) :-      % a(bort)
  abort_hook(Mode,Command), % if not implemented or fails for another reason, we
  !.                        % set the command action variable to abort in the
                            % next clause
action_mode_command(97,debug,abort,_Inv,_Port,_Autoskip) :-
  !.
action_mode_command(_,skip(Inv),proceed,Inv,call,true) :-         % autoskip at call ports
  !.
action_mode_command(_,skip(Inv),proceed,Inv,redo,true) :-         % autoskip at redo ports
  !.
action_mode_command(_,debug,proceed,_Inv,_Port,_Autoskip).        % creep

:- multifile post_step_hook/5.

% Calls all clauses of kahinasicstus:post_step_hook/5 in a failure-driven loop.
% Modules can add such clauses e.g. to read additional information from a goal
% before it is called, and pass it to Kahina. 
run_post_step_hooks(Bridge,JVM,Inv,Pred,GoalDesc) :-
  post_step_hook(Bridge,JVM,Inv,Pred,GoalDesc),
  fail.
run_post_step_hooks(_,_,_,_,_).

:- multifile pre_exit_hook/5.

% Calls all clauses of kahinasicstus:post_step_hook/5 in a failure-driven loop.
% Modules can add such clauses e.g. to read additional information from a goal
% after it exits, and pass it to Kahina. 
run_pre_exit_hooks(Bridge,JVM,Inv,Det,GoalDesc) :-
  pre_exit_hook(Bridge,JVM,Inv,Det,GoalDesc),
  fail.
run_pre_exit_hooks(_,_,_,_,_).

:- multifile classpath_element/1.

classpath(Classpath) :-
  bagof(Element,classpath_element(Element),Classpath),
  !.
classpath([]).

% ------------------------------------------------------------------------------
% TRACING
% Predicates that define what kahinasicstus does at individual ports.
% TODO This part, especially act/5, has a lot of redundancy, is on the verge of
% becoming a maintenance nightmare and needs refactoring.
% ------------------------------------------------------------------------------

:- dynamic unblocked_pseudostep_waiting_for_link/1.

act(call,Inv,Bridge,JVM,Options) :-
  retract(unblock_pseudostep_waiting_for_link(UnblockingID)),
  execution_state(goal(Module:Goal)),
  recall_blocked_goal(Module:Goal,BlockingID),
  !,
  link_nodes(Bridge,JVM,UnblockingID,BlockingID),
  act(call,Inv,Bridge,JVM,Options). % Continue with second clause of act/5. Can't just fail because recall_blocked_goal/2 is supposed to change the execution state.
act(call,Inv,Bridge,JVM,Options) :-
  top_start(Inv),
  execution_state(pred(Module:Pred)),	% "module qualified goal template", see manual
  write_term_to_chars(Module:Pred,PredChars,[max_depth(5)]),
  execution_state(goal(_:Goal)),
  goal_desc(Module,Goal,Options,GoalDesc),
  write_term_to_chars(GoalDesc,GoalDescChars,[max_depth(5)]),
  act_step(Bridge,JVM,Inv,PredChars,GoalDescChars),
  run_post_step_hooks(Bridge,JVM,Inv,PredChars,GoalDesc),
  % TODO make the following into hooks
  act_source_code_location(Bridge,JVM,Inv,Options),
  (memberchk(layer(Layer),Options),
   integer(Layer)
   -> register_layer(Bridge,JVM,Inv,Layer)
    ; true),
  act_call(Bridge,JVM,Inv),
  perhaps(send_variable_bindings(Bridge,JVM,Inv,call)).
act(fail,Inv,Bridge,JVM,Options) :-
  retractall(unblock_pseudostep_waiting_for_link(_)),
  act_fail(Bridge,JVM,Inv),
  act_source_code_location(Bridge,JVM,Inv,Options),
  top_end(Inv,Bridge,JVM).
act(exit(DetFlag),Inv,Bridge,JVM,Options) :-
  retractall(unblock_pseudostep_waiting_for_link(_)),
  execution_state(pred(Module:_)),
  execution_state(goal(_:Goal)),
  goal_desc(Module,Goal,Options,GoalDesc),
  run_pre_exit_hooks(Bridge,JVM,Inv,Det,GoalDesc),
  % TODO make the following into hooks
  act_source_code_location(Bridge,JVM,Inv,Options),
  perhaps(send_variable_bindings(Bridge,JVM,Inv,exit(DetFlag))),
  write_term_to_chars(GoalDesc,GoalDescChars,[max_depth(5)]),
  detflag_det(DetFlag,Det), % translates det/nondet to true/false
  act_exit(Bridge,JVM,Inv,Det,GoalDescChars),
  top_end(Inv,Bridge,JVM).
act(redo,Inv,Bridge,JVM,Options) :-
  top_start(Inv),
  retractall(unblock_pseudostep_waiting_for_link(_)),
  act_redo(Bridge,JVM,Inv),
  act_source_code_location(Bridge,JVM,Inv,Options).
act(exception(Exception),Inv,Bridge,JVM,Options) :-
  retractall(unblock_pseudostep_waiting_for_link(_)),
  write_term_to_chars(Exception,ExceptionChars,[max_depth(5)]),
  act_exception(Bridge,JVM,Inv,ExceptionChars),
  act_source_code_location(Bridge,JVM,Inv,Options),
  top_end(Inv,Bridge,JVM).
act(block,Inv,Bridge,JVM,Options) :-
  retractall(unblock_pseudostep_waiting_for_link(_)), % TODO What if the unblocked step is immediately blocked, e.g. in freeze(X,freeze(Y,...))? freeze/2 isn't called, so we would have to do the linking here.
  execution_state(goal(Module:Goal)),
  remember_blocked_goal(Module:Goal,ID),
  execution_state(pred(_:Pred)),
  write_term_to_chars(Module:Pred,PredChars,[max_depth(5)]),
  goal_desc(Module,Goal,GoalDesc),
  write_term_to_chars(GoalDesc,GoalDescChars,[max_depth(5)]),
  act_step(Bridge,JVM,ID,[98,108,111,99,107,32|PredChars],[98,108,111,99,107,32|GoalDescChars]), % 'block '
  (memberchk(layer(Layer),Options),
   integer(Layer)
   -> register_layer(Bridge,JVM,Inv,Layer)
    ; true),
  act_call(Bridge,JVM,ID),
  act_exit(Bridge,JVM,ID,true).
act(unblock,Inv,Bridge,JVM,Options) :-
  retractall(unblock_pseudostep_waiting_for_link(_)),
  get_next_pseudostep_id(ID),
  execution_state(pred(Module:Pred)),
  write_term_to_chars(Module:Pred,PredChars,[max_depth(5)]),
  execution_state(goal(_:Goal)),
  goal_desc(Module,Goal,Options,GoalDesc),
  write_term_to_chars(GoalDesc,GoalDescChars,[max_depth(5)]),
  act_step(Bridge,JVM,ID,[117,110,98,108,111,99,107,32|PredChars],[117,110,98,108,111,99,107,32|GoalDescChars]), % 'unblock '
  (memberchk(layer(Layer),Options),
   integer(Layer)
   -> register_layer(Bridge,JVM,Inv,Layer)
    ; true),
  act_call(Bridge,JVM,ID),
  act_exit(Bridge,JVM,ID,true),
  % Would be nicer to have the unblocked steps as children of the unblock step
  % rather than siblings, but we don't know how many there are.
  assert(unblock_pseudostep_waiting_for_link(ID)).

% This is currently called from act/5 at every port except block, unblock ports.
% For non-custom source code locations, it would suffice to do it at call ports
% since they are always the same for all ports of a procedure box. If this is
% believed to be a performance issue (Jasper calls are relatively expensive),
% future developers might want to devise something clever to avoid it.
act_source_code_location(Bridge,JVM,Inv,Options) :-
  ((memberchk(source_code_location(Callback,File,Line),Options),
    Callback
    -> true
     ; execution_state(line(File,Line))	% The source_info flag has to be set to on or to emacs and not all goals have line information associated with them.
       -> true
        ; fail)
   -> write_to_chars(File,FileChars),
      register_source_code_location(Bridge,JVM,Inv,FileChars,Line)
    ; true).

:- dynamic top/1.

top_start(_) :-
  top(_),
  !.
top_start(Inv) :-
  assert(top(Inv)).

top_end(Inv,Bridge,JVM) :-
  retract(top(Inv)),
  !,
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','select',[instance]),
      select(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer),
      select(Bridge,1)).
top_end(_,_,_).

act_step(Bridge,JVM,Inv,PredChars,GoalDescChars) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','step',[instance]),
      step(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+chars,+chars),
      step(Bridge,Inv,PredChars,GoalDescChars)).

act_call(Bridge,JVM,Inv) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','call',[instance]),
      call(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer),
      call(Bridge,Inv)).

act_fail(Bridge,JVM,Inv) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','fail',[instance]),
      fail(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer),
      fail(Bridge,Inv)).

act_exception(Bridge,JVM,Inv,ExceptionChars) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','exception',[instance]),
      exception(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+chars),
      exception(Bridge,Inv,ExceptionChars)).

act_exit(Bridge,JVM,Inv,Det) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','exit',[instance]),
      exit(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+boolean),
      exit(Bridge,Inv,Det)).

act_exit(Bridge,JVM,Inv,Det,GoalDescChars) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','exit',[instance]),
      exit(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+boolean,+chars),
      exit(Bridge,Inv,Det,GoalDescChars)).

act_redo(Bridge,JVM,Inv) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','redo',[instance]),
      redo(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer),
      redo(Bridge,Inv)).

register_source_code_location(Bridge,JVM,Inv,FileChars,Line) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','registerStepSourceCodeLocation',[instance]),
      register_step_source_code_location(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+chars,+integer),
      register_step_source_code_location(Bridge,Inv,FileChars,Line)).

register_layer(Bridge,JVM,Inv,Layer) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','registerLayer',[instance]),
      register_layer(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+integer),
      register_layer(Bridge,Inv,Layer)).

link_nodes(Bridge,JVM,Anchor,Target) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','linkNodes',[instance]),
      link_nodes(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+integer),
      link_nodes(Bridge,Anchor,Target)).

send_variable_bindings(Bridge,JVM,Inv,Port) :-
  variable_binding(Name,Value), % has many solutions
  send_variable_binding(Bridge,JVM,Inv,Port,Name,Value),
  fail.
send_variable_binding(_,_,_).

send_variable_binding(Bridge,JVM,Inv,Port,Name,Value) :-
  port_direction(Port,DirectionChars),
  write_to_chars(Name,NameChars),
  write_term_to_chars(Value,ValueChars,[max_depth(5)]),
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','registerBinding',[instance]),
      register_bindings(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),+integer,+chars,+chars,+chars),
      register_bindings(Bridge,Inv,DirectionChars,NameChars,ValueChars)).

% ------------------------------------------------------------------------------
% CONTROL
% ------------------------------------------------------------------------------

get_action(unblock,_,_,99) :- % c
  !.
get_action(_,Bridge,JVM,Action) :-
  wait_for_action(Bridge,JVM,Action),
  !.

wait_for_action(Bridge,JVM,Action) :-
  repeat,
  get_action_from_bridge(Bridge,JVM,Action),
  (Action == 110 % n
  -> (sleep(0.1),
      fail)
   ; true).

get_action_from_bridge(Bridge,JVM,Action) :-
  jasper_call(JVM,
      method('org/kahina/sicstus/bridge/SICStusPrologBridge','getAction',[instance]),
      get_action(+object('org/kahina/sicstus/bridge/SICStusPrologBridge'),[-char]),
      get_action(Bridge,Action)).

% ------------------------------------------------------------------------------
% INSTANCE/SESSION/BRIDGE MANAGEMENT
% ------------------------------------------------------------------------------

:- dynamic bridge/1.

get_bridge(1,call,Bridge) :-
  !,
  get_jvm(JVM),
  start_new_kahina_session(Bridge,JVM).
get_bridge(_,_,Bridge) :-
  bridge(Bridge),
  !.
get_bridge(_,_,Bridge) :-
  get_jvm(JVM),
  start_new_kahina_session(Bridge,JVM).

% Since we use Prolog's invocation numbers for step IDs, we need a
% non-conflicting number space for pseudostep IDs... like negative numbers.

:- dynamic next_pseudostep_id/1.

initialize_pseudostep_id :-
  retractall(next_pseudostep_id(_)),
  assert(next_pseudostep_id(-2)). % start here because -1 is sometimes used as a null equivalent

get_next_pseudostep_id(ID) :-
  retract(next_pseudostep_id(ID)),
  NewID is ID - 1,
  assert(next_pseudostep_id(NewID)).

start_new_kahina_session(Bridge,JVM) :-
  end_trace_session,
  retractall(top(_)),
  initialize_pseudostep_id,
  retractall(source_read(_)),
  retractall(source_clause(_,_,_,_)),
  get_kahina_instance(Instance,JVM),
  catch(
      jasper_call(JVM,
              method('org/kahina/sicstus/SICStusPrologDebuggerInstance','startNewSession',[instance]),
              start_new_session(+object('org/kahina/sicstus/SICStusPrologDebuggerInstance'),[-object('org/kahina/sicstus/bridge/SICStusPrologBridge')]),
              start_new_session(Instance,LocalBridge)),
      E,
      (is_java_exception(JVM,E)
      -> print_exception_info(JVM,E)
       ; raise(E))),
  jasper_create_global_ref(JVM,LocalBridge,Bridge),
  assert(bridge(Bridge)),
  write_to_chars('[query]',RootLabelChars),
  act_step(Bridge,JVM,0,RootLabelChars,RootLabelChars),
  register_layer(Bridge,JVM,0,0),
  act_call(Bridge,JVM,0).

% retract and delete all bridge references
end_trace_session :-
  retract(bridge(Bridge)),
  get_jvm(JVM),
  jasper_delete_global_ref(JVM,Bridge),
  fail.
end_trace_session.

:- dynamic kahina_instance/1.

get_kahina_instance(Instance,_) :-
  kahina_instance(Instance),
  !.
get_kahina_instance(Instance,JVM) :-
  instance_class(InstanceClass),
  jasper_new_object(JVM,InstanceClass,init,init,LocalInstance),
  jasper_create_global_ref(JVM,LocalInstance,Instance),
  assert(kahina_instance(Instance)).

:- multifile instance_class_hook/1.

instance_class(InstanceClass) :-
  instance_class_hook(InstanceClass),
  !.
instance_class('org/kahina/sicstus/SICStusPrologDebuggerInstance').

:- dynamic jvm/1.

get_jvm(JVM) :-
  jvm(JVM),
  !.
get_jvm(JVM) :-
  classpath(Classpath),
  jasper_initialize([classpath(Classpath),'-Xss2m'],JVM), % TODO classpath, heap size?
  assert(jvm(JVM)).

% ------------------------------------------------------------------------------
% SOURCE INFORMATION
% Mainly variable bindings.
% ------------------------------------------------------------------------------

:- dynamic source_read/1.
:- dynamic source_clause/5. % TODO index by functor?

variable_binding(Name,Value) :-
  execution_state(line(File,Line)), % TODO we do that twice at call ports, consolidate
  (source_read(File) % TODO is that guaranteed to be absolute?
  -> true
   ; read_source_file(File)),
  execution_state(parent_clause(_Clause,SubtermSelector)), % Clause is as it is read from the source module modulo prefix translation, it does not have variable bindings.
  execution_state(goal(_:Goal)), % TODO consolidate, see above
  once(( % TODO With two clauses on the same line, this may find the wrong one.
      source_clause(SourceClause,File,FirstLine,LastLine,Names),
      FirstLine =< Line,
      LastLine >= Line)), % TODO check if SourceClause is a variant of Clause module modulo prefix normalization
  SourceClause = (_ :- SourceBody), % if we were dealing with a fact, we wouldn't be here
  select_subterm(SubtermSelector,SourceBody,SourceGoal),
  term_variables(SourceGoal,Variables), % limit our attention to the variables in the current goal, for the others we don't have the values handy
  member(Variable,Variables), % pick a variable
  member(Name=Value,Names), % pick a name
  Variable == Value, % match them
  (SourceGoal = Goal % bind current value to variable
  -> true
   ; SourceGoal = _:Goal). % in case the source form of the goal has a module prefix

read_source_file(AbsFileName) :-
  open(AbsFileName,read,Stream,[eof_action(eof_code)]),
  repeat,
    read_term(Stream,Term,[variable_names(Names),layout(Layout)]),
    handle_term(Term,Names,Layout,AbsFileName),
  !,
  close(Stream),
  assert(source_read(AbsFileName)).

handle_term(end_of_file,_,_,_) :-
  !.
handle_term(Clause,Names,Layout,AbsFileName) :-
  first_line(Layout,FirstLine),
  last_line(Layout,LastLine),
  assert(source_clause(Clause,AbsFileName,FirstLine,LastLine,Names)),
  fail.

% ------------------------------------------------------------------------------
% BLOCKED GOALS
% Here, unblock steps are matched to corresponding block steps in the history.
% Some guesswork is involved.
% ------------------------------------------------------------------------------

% analyze_blocked_goal(+Goal,-Condition)
% For a given blocked goal, gives in the form of a goal a necessary (but alas,
% not sufficient) condition for it to be unblocked.
analyze_blocked_goal(_:when(Condition,Goal),Condition,Goal) :-
  !.
analyze_blocked_goal(_:freeze(Var,Goal),nonvar(Var),Goal) :-
  !.
analyze_blocked_goal(_:dif(X,Y),?=(X,Y),0) :- % no goal - what TODO about this?
  !.
analyze_blocked_goal(Module:Goal,true,Module:Goal).
% The last clause is for goals blocked by block declarations. Here we don't know
% the exact condition since we don't know which BlockSpec caused the blocking.
% We could of course apply a tighter heuristic and require that there is at
% least one BlockSpec that evaluates to true at block time such that one of the
% arguments it marks by - is bound at unblock time. But I guess this would in
% practice lead to only very few additional correct guesses.

remember_blocked_goal(Module:Goal,ID) :-
  analyze_blocked_goal(Module:Goal,Condition,TargetModule:TargetGoal), % could fail if we used the tighter heuristic
  get_next_pseudostep_id(ID),
  memory(blocked_goals(BlockedGoals)),
  (reduce_goal(TargetGoal,TargetModule,ReducedTarget)
  -> memberchk(blocked_goal(ID,ReducedTarget,Condition,_),BlockedGoals) % last arg will be instantiated later to mark goal as unblocked
   ; true). % We will not be able to trace any unblock step back to this block step.

% recall_blocked_goal(+Goal,-ID)
% To be called when Goal is unblocked. Succeeds if it was previously remembered
% blocked, and instantiates ID to the ID of the corresponding block pseudostep.
recall_blocked_goal(Module:Goal,ID) :-
  memory(blocked_goals(BlockedGoals)),
  member(blocked_goal(ID,Module2:Goal2,Condition,Unblocked),BlockedGoals),
  (var(ID) % end of open-ended list reached
  -> !, fail
   ; true),
  var(Unblocked), % not unblocked yet in the current stack
  % Check if it's the same goal. If two literally identical goals are blocked,
  % and unblocked in reverse order, our guess at the ID will be wrong. Checking
  % the unblock condition might prevent this but cannot fully do so because it
  % is not always sufficient, and does not help when both conditions become true
  % simultaneously. In the latter case, we can hope that goals are unblocked in
  % the order they were blocked, but SP does not guarantee this.
  Module == Module2,
  Goal == Goal2,
  Condition,
  Unblocked = true,
  !.

reduce_goal(Goal,Module,Reduced) :-
  goal_behavior(Goal,Module,call(Reduced)).

% goal_behavior(+Goal,+Module,-Behavior)
% Predicts the behavior of a given goal when called in trace mode, taking into
% account the fact that the control predicates !/0, true/0, fail/0, ,/2, ->/2,
% ;/2, if/3 and once/1 do not show up in the trace. Possible resulting values
% for Behavior are:
%   true(cut) - the goal will succeed without trace and contains a cut
%   true(nocut) - the goal will succed without trace and contains no cut
%   fail(cut) - the goal will fail without trace and contains a cut
%   fail(nocut) - the goal will fail without trace and contains no cut
%   call(G) - the tracer will show the call port of a procedure box with goal G
% Clauses are grouped by the form of Goal.
% Module:Goal
goal_behavior(Module:Goal,_,Behavior) :-
  !,
  goal_behavior(Goal,Module,Behavior).
% !
goal_behavior(!,_,true(cut)) :-
  !.
% true
goal_behavior(true,_,true(nocut)) :-
  !.
% fail
goal_behavior(fail,_,fail(nocut)) :-
  !.
% A,B
goal_behavior((A,_),Module,fail(Cut)) :-
  goal_behavior(A,Module,fail(Cut)),
  !.
goal_behavior((A,B),Module,Behavior) :-
  goal_behavior(A,Module,true(Cut)),
  !,
  goal_behavior(B,Module,Behavior0),
  add_cut(Behavior0,Cut,Behavior).
goal_behavior((A,_),Module,Behavior) :-
  !,
  goal_behavior(A,Module,Behavior).
% A -> B
goal_behavior((A->_),Module,fail(nocut)) :-
  goal_behavior(A,Module,fail(_)), % scope of cut limited to A
  !.
goal_behavior((A->B),Module,Behavior) :-
  goal_behavior(A,Module,true(_)), % scope of cut limited to A
  !,
  goal_behavior(B,Module,Behavior).
goal_behavior((A->_),Module,Behavior) :-
  !,
  goal_behavior(A,Module,Behavior).
% A;B
goal_behavior((A;_),Module,fail(cut)) :-
  goal_behavior(A,Module,fail(cut)),
  !.
goal_behavior((A;B),Module,Behavior) :-
  goal_behavior(A,Module,fail(nocut)),
  !,
  goal_behavior(B,Module,Behavior).
goal_behavior((A;_),Module,true(Cut)) :-
  goal_behavior(A,Module,true(Cut)),
  !.
goal_behavior((A;_),Module,Behavior) :-
  !,
  goal_behavior(A,Module,Behavior).
% \+A
goal_behavior(\+A,Module,true(nocut)) :-
  !,
  goal_behavior(A,Module,fail(_)). % scope of cut limited to A
goal_behavior(\+A,Module,fail(nocut)) :-
  !,
  goal_behavior(A,Module,true(_)). % scope of cut limited to A
goal_behavior(\+A,Module,Behavior) :-
  !,
  goal_behavior(A,Module,Behavior).
% if(A,B,C)
goal_behavior(if(A,_,C),Module,Behavior) :-
  goal_behavior(A,Module,fail(_)), % scope of cut limited to A
  !,
  goal_behavior(C,Module,Behavior).
goal_behavior(if(A,B,_),Module,Behavior) :-
  goal_behavior(A,Module,true(_)), % scope of cut limited to A
  !,
  goal_behavior(B,Module,Behavior).
goal_behavior(if(A,_,_),Module,Behavior) :-
  !,
  goal_behavior(A,Module,Behavior).
% once(A)
goal_behavior(once(A),Module,fail(nocut)) :-
  goal_behavior(A,Module,fail(_)), % scope of cut limited to A
  !.
goal_behavior(once(A),Module,true(nocut)) :-
  goal_behavior(A,Module,true(_)), % scope of cut limited to A
  !.
goal_behavior(once(A),Module,Behavior) :-
  !,
  goal_behavior(A,Module,Behavior).
% other forms
goal_behavior(Goal,Module,call(Module:Goal)).

add_cut(true(Cut1),Cut2,true(Cut)) :-
  cut_or(Cut1,Cut2,Cut).
add_cut(fail(Cut1),Cut2,fail(Cut)) :-
  cut_or(Cut1,Cut2,Cut).
add_cut(call(Goal),_,call(Goal)).

cut_or(nocut,nocut,nocut) :-
  !.
cut_or(_,_,cut).

% ------------------------------------------------------------------------------
% UTILITIES
% ------------------------------------------------------------------------------

memory(Term) :-
  execution_state(private(P)),
  memberchk(Term,P).

port_direction(call,[105,110]). % in
port_direction(fail,[111,117,116]). % out
port_direction(redo,[105,110]).
port_direction(exit(_),[111,117,116]).
port_direction(exception,[111,117,116]).

% try to prove Goal, but succeed even if that fails
perhaps(Goal) :-
  Goal,
  !.
perhaps(_).

first_line([FirstLine|_],FirstLine) :-
  !.
first_line(FirstLine,FirstLine).

last_line(LastLine,LastLine) :-
  integer(LastLine),
  !.
last_line([LastArg],LastLine) :-
  !,
  last_line(LastArg,LastLine).
last_line([_|Args],LastLine) :-
  last_line(Args,LastLine).

% select_subterm(+SubtermSelector,?Term,?Subterm)
select_subterm([],Term,Term).
select_subterm([ArgNo|ArgNos],Term,Subterm) :-
  arg(ArgNo,Term,Arg),
  select_subterm(ArgNos,Arg,Subterm).

% goal_desc(+Module,+Goal,+Options,-GoalDesc)
% maps goals to goal descriptions for display
goal_desc(Module,Goal,Options,Desc) :-
  member(goal_desc(Callback,Module,Goal,Desc),Options),
  Callback,
  !.
% default: goal without module prefix
goal_desc(_,Goal,_,Goal).

detflag_det(det,true).
detflag_det(nondet,false).

memberchkid(Element,List) :-
  is_list(List),
  memberchkid_act(Element,List).

memberchkid_act(Element,[First|_]) :-
  Element == First,
  !.
memberchkid_act(Element,[_|Rest]) :-
  memberchkid_act(Element,Rest).

% ------------------------------------------------------------------------------
% EXCEPTION HANDLING
% from SICStus manual
% ------------------------------------------------------------------------------

is_java_exception(_JVM, Thing) :- var(Thing), !, fail.
is_java_exception(_JVM, Thing) :-
   Thing = java_exception(_),      % misc error in Java/Prolog glue
   !.
is_java_exception(JVM, Thing) :-
   jasper_is_object(JVM, Thing),
   jasper_is_instance_of(JVM, Thing, 'java/lang/Throwable').

print_exception_info(_JVM, java_exception(Message)) :- !,
   format(user_error, '~NJasper exception: ~w~n', [Message]).
print_exception_info(JVM, Excp) :-
   /*
   // Approximate Java code
   {
      String messageChars = excp.getMessage();
   }
   */
   jasper_call(JVM,
               method('java/lang/Throwable', 'getMessage', [instance]),
               get_message(+object('java/lang/Throwable'), [-chars]),
               get_message(Excp, MessageChars)),
   /* // Approximate Java code
   {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      excp.printStackTrace(printWriter);
      printWriter.close();
      stackTraceChars = StringWriter.toString();
   }
   */
   jasper_new_object(JVM, 'java/io/StringWriter',
                     init, init, StringWriter),
   jasper_new_object(JVM, 'java/io/PrintWriter',
                     init(+object('java/io/Writer')),
                     init(StringWriter), PrintWriter),
   jasper_call(JVM,
               method('java/lang/Throwable', 'printStackTrace', [instance]),
               print_stack_trace(+object('java/lang/Throwable'),
                                 +object('java/io/PrintWriter')),
               print_stack_trace(Excp, PrintWriter)),
   jasper_call(JVM,
               method('java/io/PrintWriter','close',[instance]),
               close(+object('java/io/PrintWriter')),
               close(PrintWriter)),
   jasper_call(JVM,
               method('java/io/StringWriter','toString',[instance]),
               to_string(+object('java/io/StringWriter'),[-chars]),
               to_string(StringWriter, StackTraceChars)),
   jasper_delete_local_ref(JVM, PrintWriter),
   jasper_delete_local_ref(JVM, StringWriter),
   %% ! exceptions are thrown as global references
   jasper_delete_global_ref(JVM, Excp),
   format(user_error, '~NJava Exception: ~s\nStackTrace: ~s~n',
          [MessageChars, StackTraceChars]).

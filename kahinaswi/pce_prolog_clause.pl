/*  This file is originally part of XPCE --- The SWI-Prolog GUI toolkit

    Author:        Jan Wielemaker and Anjo Anjewierden
    E-mail:        wielemak@science.uva.nl
    WWW:           http://www.swi.psy.uva.nl/projects/xpce/
    Copyright (C): 1985-2006, University of Amsterdam

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

    As a special exception, if you link this library with other files,
    compiled with a Free Software compiler, to produce an executable, this
    library does not by itself cause the resulting executable to be covered
    by the GNU General Public License. This exception does not however
    invalidate any other reasons why the executable file might be covered by
    the GNU General Public License.
*/

:- module(pce_prolog_clause,
	  [ pce_clause_info/4,		% +ClauseRef, -File, -TermPos, -VarNames
	    clear_clause_info_cache/0,
	    predicate_classification/2	% +Goal, -Classification
	  ]).
:- use_module(library(pce)).
:- use_module(library(debug)).
:- use_module(library(listing)).
:- use_module(library(prolog_clause)).

:- pce_global(@dynamic_source_buffer,
	      make_dynamic_source_buffer).

make_dynamic_source_buffer(B) :-
	start_emacs,
	new(B, emacs_buffer(@nil, '*dynamic code*')).

		 /*******************************
		 *	       CACHE		*
		 *******************************/

:- dynamic
	clause_info_cache/4.
:- multifile
	user:prolog_event_hook/1.

user:prolog_event_hook(erased(Ref)) :-
	retract(clause_info_cache(Ref, _, _, _)),
	debug(clause_info, 'Retracted info for ~d~n', [Ref]),
	fail.				% allow other hooks

clear_clause_info_cache :-
	retractall(clause_info_cache(_, _, _, _)).

%	clause_info(+ClauseRef, -File, -TermPos, -VarNames)
%
%	Fetches source information for the given clause.

pce_clause_info(ClauseRef, File, TermPos, NameOffset) :-
	clause_info_cache(ClauseRef, File, TermPos, NameOffset), !,
	debug(clause_info, 'clause_info(~w): from cache~n', [ClauseRef]).
pce_clause_info(ClauseRef, File, TermPos, NameOffset) :-
	clause_info(ClauseRef, File, TermPos, NameOffset), !,
	asserta(clause_info_cache(ClauseRef, File, TermPos, NameOffset)),
	debug(clause_info, 'Added to info-cache', []).
pce_clause_info(ClauseRef, S, TermPos, NameOffset) :-
	debug(clause_info, 'Listing for clause ~w', [ClauseRef]),
	'$clause'(Head, Body, ClauseRef, VarOffset),
	(   Body == true
	->  Clause = Head
	;   Clause = (Head :- Body)
	),
	start_emacs,
	S = @dynamic_source_buffer,
	clause_name(ClauseRef, ClauseName),
	send(S, attribute, comment,
	     string('Decompiled listing of %s', ClauseName)),
	send(S, clear),
	debug(clause_info, 'Writing clause ~w to string ~p ... ', [ClauseRef, S]),
	pce_open(S, write, Fd),
	portray_clause(Fd, Clause),
	close(Fd),
	debug(clause_info, 'ok, reading ... ', []),
	pce_open(S, read, Handle),
	call_cleanup(prolog_clause:read(Handle, user,
					ReadClause, TermPos, VarNames),
		     close(Handle)),
	prolog_clause:unify_term(Clause, ReadClause),
	debug(clause_info, 'ok ...', []),
	prolog_clause:make_varnames(Clause, VarOffset, VarNames, NameOffset),
	debug(clause_info, 'got names~n', []), !.


predicate_classification(Goal, Style) :-
	predicate_property(Goal, Prop),
	map_property(Prop, Style), !.
predicate_classification(_, user).

style_property(built_in).
style_property(foreign).
style_property(dynamic).
style_property(undefined).
style_property(transparent).

map_property(Prop, Prop) :-
	style_property(Prop), !.



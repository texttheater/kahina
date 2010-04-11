package org.kahina.core.data;


/**
 * A data manager allows clients to store and retrieve pieces of data (e.g.
 * information associated with debugger steps) in the form of
 * {@link KahinaObject}s. Such an object is uniquely identified by its data type
 * (i.e. its runtime class, i.e. some subclass of {@link KahinaObject}), and its
 * ID ({@link KahinaObject.getID()}).
 * 
 * Within a data manager, each data type has its own {@link DataStore}
 * responsible for storing and retrieving instances of that data type. For each
 * data type, clients can either register their own store or let the data
 * manager create a default store. The client is responsible for ensuring that
 * for each data type that a data manager is used with, a <b>suitable</b> data
 * store is registered.
 * 
 * @author ke
 * 
 */
public abstract class DataManager
{

    /**
     * Returns the data store that is registered for a given data type.
     *
     * @param type
     * @return
     */
    protected abstract DataStore getStoreForType(Class<? extends KahinaObject> type);

    /**
     * Registers a given data store for a given data type. Only one data store
     * can be registered per data type. The data store should be suitable for
     * the data type.
     *
     * @param type
     * @param store
     */
    public abstract void registerDataType(Class<? extends KahinaObject> type, DataStore store);

    /**
     * Registers a data type and let the data manager pick a data store. Clients
     * should make sure the data manager can pick a suitable store for the data
     * type.
     *
     * @param type
     */
    public abstract void registerDataType(Class<? extends KahinaObject> type);

    /**
     * Stores an object. The data type (i.e. runtime class) of the object must
     * have been registered with this data manager before.
     * @param object
     */
    public void store(KahinaObject object)
    {
        getStoreForType(object.getClass()).store(object);
    }

    /**
     * Retrieves a stored object by its data type and ID.
     * @param type - the type of the desired object (static class field of a class extending KahinaObject)
     * @param id - the type-specific ID of the desired object
     * @return the desired object as a KahinaObject which can be cast to the original type
     */
    @SuppressWarnings("unchecked")
	public <T extends KahinaObject> T retrieve(Class<T> type, int id)
    {
        return (T) getStoreForType(type).retrieve(id);
    }

    /**
     * This method must be called to persist the stored objects across
     * runtimes.
     * @throws UnsupportedOperationException if this data manager does not
     * support persistence.
     */
    public void persist()
    {
        throw new UnsupportedOperationException("This data manager does not support persistence.");
    }
}

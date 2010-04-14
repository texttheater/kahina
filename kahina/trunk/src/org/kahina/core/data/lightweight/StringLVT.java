package org.kahina.core.data.lightweight;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.kahina.core.data.KahinaObject;

public class StringLVT extends LVT
{
	public static StringLVT createStringLVT(Type type)
	{
		if (type == String.class)
		{
			return new StringLVT();
		}
		return null;
	}

	@Override
	void retrieveFieldValue(int objectID, int fieldID, Field field, KahinaObject object,
			LightweightDbStore store) throws IllegalAccessException
	{
		field.set(object, store.retrieveLongVarchar(objectID, fieldID));
	}

	@Override
	void storeFieldValue(int objectID, int fieldID, Field field,
			KahinaObject object, LightweightDbStore store) throws IllegalAccessException
	{
		store.storeLongVarchar(objectID, fieldID, (String) field.get(object));
	}

}

package org.brandao.brutos.annotation.helper.any.app2.metavaluesdefinition;

import java.util.ArrayList;
import java.util.List;

import org.brandao.brutos.annotation.configuration.MetaValueDefinition;
import org.brandao.brutos.annotation.configuration.MetaValuesDefinition;
import org.brandao.brutos.annotation.helper.any.app2.DecimalProperty;
import org.brandao.brutos.annotation.helper.any.app2.SetProperty;

public class TestDateMetaValuesDefinition 
	implements MetaValuesDefinition{

	public List<MetaValueDefinition> getMetaValues() {
		List<MetaValueDefinition> list = new ArrayList<MetaValueDefinition>(); 
		list.add(new MetaValueDefinition("2015-01-01", DecimalProperty.class));
		list.add(new MetaValueDefinition("2015-01-02", SetProperty.class));
		return list;
	}

}

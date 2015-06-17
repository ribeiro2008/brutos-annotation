package org.brandao.brutos.annotation.helper.any.app1;

import org.brandao.brutos.annotation.Any;
import org.brandao.brutos.annotation.Basic;
import org.brandao.brutos.annotation.Controller;
import org.brandao.brutos.annotation.EnumerationType;
import org.brandao.brutos.annotation.MetaValue;

@Controller("/controller")
public class Test3AnyController {

	@Basic(bean="propertyA")
	@Any(
		metaBean=@Basic(bean="propertyType"),
		metaType=PropertyType.class,
		metaEnumerated=EnumerationType.STRING,
		metaValues={
			@MetaValue(name="DECIMAL", target=DecimalProperty.class),
			@MetaValue(name="SET", target=SetProperty.class)
		}
	)
	public Property property1;
	
	private Property property2;

	public Property getProperty2() {
		return property2;
	}

	@Basic(bean="propertyB")
	@Any(
		metaBean=@Basic(bean="propertyType2"),
		metaType=PropertyType.class,
		metaEnumerated=EnumerationType.STRING,
		metaValues={
			@MetaValue(name="DECIMAL", target=DecimalProperty.class),
			@MetaValue(name="SET", target=SetProperty.class)
		}
	)
	public void setProperty2(Property property2) {
		this.property2 = property2;
	}
	
	
}
package org.brandao.brutos.annotation.configuration;

import java.util.List;

import org.brandao.brutos.BrutosException;
import org.brandao.brutos.ClassUtil;
import org.brandao.brutos.ComponentRegistry;
import org.brandao.brutos.ConstructorArgBuilder;
import org.brandao.brutos.ControllerBuilder;
import org.brandao.brutos.EnumerationType;
import org.brandao.brutos.MetaBeanBuilder;
import org.brandao.brutos.ParameterBuilder;
import org.brandao.brutos.PropertyBuilder;
import org.brandao.brutos.ScopeType;
import org.brandao.brutos.annotation.Any;
import org.brandao.brutos.annotation.Enumerated;
import org.brandao.brutos.annotation.Identify;
import org.brandao.brutos.annotation.MetaValue;
import org.brandao.brutos.annotation.Stereotype;
import org.brandao.brutos.annotation.Temporal;
import org.brandao.brutos.annotation.Transient;
import org.brandao.brutos.annotation.Type;
import org.brandao.brutos.annotation.bean.BeanPropertyAnnotation;
import org.brandao.brutos.mapping.MappingException;
import org.brandao.brutos.mapping.StringUtil;

@Stereotype(target=Any.class,executeAfter={Identify.class})
public class AnyAnnotationConfig extends AbstractAnnotationConfig{

    public boolean isApplicable(Object source) {
    	
        boolean applicable = source instanceof ActionParamEntry && 
        		((ActionParamEntry)source).isAnnotationPresent(Any.class);
        
        applicable = applicable || 
                (source instanceof BeanPropertyAnnotation && 
                !((BeanPropertyAnnotation)source).isAnnotationPresent(Transient.class) &&
                ((BeanPropertyAnnotation)source).isAnnotationPresent(Any.class));
        
        applicable = applicable || (source instanceof ConstructorArgEntry && 
        		((ConstructorArgEntry)source).isAnnotationPresent(Any.class));
        
        return applicable;
    }

    public Object applyConfiguration(Object source, Object builder, 
            ComponentRegistry componentRegistry) {
    
        try{
            return applyConfiguration0(source, builder, componentRegistry);
        }
        catch(Exception e){
            
            String type = "source";
            String name = "it is a bug";
            
            if(source instanceof ActionParamEntry){
                type = "parameter";
                name = ((ActionParamEntry)source).getName();
            }
            else
            if(source instanceof BeanPropertyAnnotation){
                type = "property";
                name = ((BeanPropertyAnnotation)source).getName();
            }
            else
            if(source instanceof ConstructorArgEntry){
                type = "constructor arg";
                name = ((ConstructorArgEntry)source).getName();
            }
            
            throw 
                new BrutosException(
                        "can't identify " + type + ": " + name,
                        e );
        }
        
    }
    
    public Object applyConfiguration0(Object source, Object builder, 
            ComponentRegistry componentRegistry) throws InstantiationException, IllegalAccessException {
        
        if(source instanceof ActionParamEntry)
            addIdentify((ActionParamEntry)source, (ParameterBuilder)builder, componentRegistry);
        else
        if(source instanceof BeanPropertyAnnotation)
            addIdentify((BeanPropertyAnnotation)source, (PropertyBuilder)builder, componentRegistry);
        else
        if(source instanceof ConstructorArgEntry)
            addIdentify((ConstructorArgEntry)source, (ConstructorArgBuilder)builder, componentRegistry);
            
        return source;
    }
    
    protected void addIdentify(ActionParamEntry source, ParameterBuilder paramBuilder,
            ComponentRegistry componentRegistry) throws InstantiationException, IllegalAccessException{

        Any any = source.getAnnotation(Any.class);
        Identify identify = any.metaBean();
        Class<?> classType = any.metaType();
        
        String name = 
        		StringUtil.isEmpty(identify.bean())?
        				(source.getName() == null? source.getDefaultName() : source.getName()) :
        				identify.bean();
        				
        ScopeType scope = AnnotationUtil.getScope(identify);
        EnumerationType enumProperty = AnnotationUtil.getEnumerationType(source.getAnnotation(Enumerated.class));
        String temporalProperty = AnnotationUtil.getTemporalProperty(source.getAnnotation(Temporal.class));
        org.brandao.brutos.type.Type type = AnnotationUtil.getTypeInstance(source.getAnnotation(Type.class));
        
        MetaBeanBuilder builder = 
        		paramBuilder.buildMetaBean(name, scope, enumProperty, temporalProperty, classType, type);
        
        this.buildMetaValues(any, builder, 
        		paramBuilder.getParametersBuilder().getControllerBuilder(), componentRegistry);
        
        super.applyInternalConfiguration(source, paramBuilder, componentRegistry);
        
    }
    
    protected void addIdentify(BeanPropertyAnnotation source, PropertyBuilder propertyBuilder,
            ComponentRegistry componentRegistry) throws InstantiationException, IllegalAccessException{
        
        Any any = source.getAnnotation(Any.class);
        Identify identify = any.metaBean();
        Class<?> classType = any.metaType();
    	
        String name = 
        		StringUtil.isEmpty(identify.bean())?
        				source.getName() :
        				identify.bean();
        
        ScopeType scope = AnnotationUtil.getScope(identify);
        EnumerationType enumProperty = AnnotationUtil.getEnumerationType(source.getAnnotation(Enumerated.class));
        String temporalProperty = AnnotationUtil.getTemporalProperty(source.getAnnotation(Temporal.class));
        org.brandao.brutos.type.Type type = AnnotationUtil.getTypeInstance(source.getAnnotation(Type.class));
        
        MetaBeanBuilder builder = 
        		propertyBuilder.buildMetaBean(name, scope, enumProperty, temporalProperty, classType, type);
        
        this.buildMetaValues(any, builder, 
        		propertyBuilder.getBeanBuilder().getControllerBuilder(), componentRegistry);
        
        super.applyInternalConfiguration(
                source, propertyBuilder, componentRegistry);
        
    }

    protected void addIdentify(ConstructorArgEntry source, ConstructorArgBuilder constructorArgBuilder,
            ComponentRegistry componentRegistry) throws InstantiationException, IllegalAccessException{

        Any any = source.getAnnotation(Any.class);
        Identify identify = any.metaBean();
        Class<?> classType = any.metaType();

        String name = 
        		StringUtil.isEmpty(identify.bean())?
        				(source.getName() == null? source.getDefaultName() : source.getName()) :
        				identify.bean();
        
        ScopeType scope = AnnotationUtil.getScope(identify);
        EnumerationType enumProperty = AnnotationUtil.getEnumerationType(source.getAnnotation(Enumerated.class));
        String temporalProperty = AnnotationUtil.getTemporalProperty(source.getAnnotation(Temporal.class));
        org.brandao.brutos.type.Type type = AnnotationUtil.getTypeInstance(source.getAnnotation(Type.class));
        

        MetaBeanBuilder builder = 
        		constructorArgBuilder.buildMetaBean(name, scope, enumProperty, temporalProperty, classType, type);
        
        this.buildMetaValues(any, builder, 
        		constructorArgBuilder.getConstructorBuilder().getBeanBuilder().getControllerBuilder(), componentRegistry);
        
        super.applyInternalConfiguration(source, constructorArgBuilder, componentRegistry);
        
    }

    private void buildMetaValues(Any any, MetaBeanBuilder metaBeanBuilder, 
    		ControllerBuilder controllerBuilder, ComponentRegistry componentRegistry) 
    				throws InstantiationException, IllegalAccessException{
    	
        if(any.metaValuesDefinition() == MetaValuesDefinition.class){
        	
        	if(any.metaValues().length == 0)
        		throw new MappingException("meta values is required");
        	
	        for(MetaValue value: any.metaValues()){
	        	super.applyInternalConfiguration(
	    			new ImportBeanEntry(value.target()), 
	    			controllerBuilder, 
					componentRegistry);
	        	metaBeanBuilder.addMetaValue(value.name(), AnnotationUtil.getBeanName(value.target()));
	        }
        }
        else{
        	Class<? extends MetaValuesDefinition> metaClassDefinition = any.metaValuesDefinition();
        	MetaValuesDefinition metaValuesDefinition = 
        			(MetaValuesDefinition) ClassUtil.getInstance(metaClassDefinition);

        	List<MetaValueDefinition> list = metaValuesDefinition.getMetaValues();
        	
        	if(list == null || list.isEmpty())
        		throw new MappingException("meta values cannot be empty");
        	
	        for(MetaValueDefinition value: list){
	        	super.applyInternalConfiguration(
	    			new ImportBeanEntry(value.getTarget()), 
	    			controllerBuilder, 
					componentRegistry);
	        	metaBeanBuilder.addMetaValue(value.getName(), AnnotationUtil.getBeanName(value.getTarget()));
	        }
        }
    }
}
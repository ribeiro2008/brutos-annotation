package org.brandao.brutos.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brandao.brutos.BrutosException;
import org.brandao.brutos.CodeGenerator;
import org.brandao.brutos.FetchType;
import org.brandao.brutos.ProxyFactory;
import org.brandao.brutos.type.ArrayType;
import org.brandao.brutos.type.CollectionType;
import org.brandao.brutos.type.Type;

//o métododo que acessa o nó o cria.
//o update prefix somente fica null na criação com índice
public class AbstractBeanDecoder 
	implements BeanDecoder{

	private CodeGenerator codeGenerator;

	public void setCodeGenerator(CodeGenerator value) {
		this.codeGenerator = value;
	}
	
	/* useBeanData */
	
	public Object decode(UseBeanData entity, FetchType fetchType, Object data)
			throws BeanDecoderException {
		try{
			return this.getValue(entity, fetchType, data, new StringBuilder(), new NodeBeanDecoder());
		}
		catch(Throwable e){
			throw new BeanDecoderException(e);
		}
	}

	public Object getValue(UseBeanData entity, 
			FetchType fetchType, Object data, StringBuilder path, NodeBeanDecoder parent) {

		NodeBeanDecoder node = new NodeBeanDecoder();
		node.setBeanPath(entity.getRealName());
		node.setPath(entity.getName());
		parent.addNode(entity.getRealName(), node);
		
		if(entity.getName() != null){
			path.append(entity.getName());
		}
		
		if(fetchType == null){
			fetchType = entity.getFetchType();
		}
		
		if(fetchType.equals(FetchType.LAZY)){
			ProxyFactory proxyFactory = 
					this.codeGenerator.getProxyFactory(entity.getClassType());
			return proxyFactory.getNewProxy(entity, data, this);
		}
		
		if (!entity.isNullable()) {
			if(entity.getMetaBean() != null){
				Object value = this.getValue(entity.getMetaBean(), path, node);
				return entity.getType().convert(value);
			}
			else
			if(entity.getMapping() != null) {
				Object value = this.getValue(entity.getMapping(), path, parent);
				return entity.getType().convert(value);
			}
			else
			if(entity.getStaticValue() != null){
				return entity.getType().convert(entity.getStaticValue());
			}
			else{
				Type type = entity.getType();
				Object value;
				
				if(type instanceof CollectionType || type instanceof ArrayType){
					value = 
						entity.getName() == null? 
							null : 
							entity.getScope().getCollection(path.toString());
				}
				else{
					value = 
							entity.getName() == null? 
								null : 
								entity.getScope().get(path.toString());
				}
				return type.convert(value);
			}
		}
		
		return null;
	}	

	public Object getValue(MetaBean entity, StringBuilder path, NodeBeanDecoder parent) {
		
		int len = path.length();
		
		this.updatePath(path, entity.getSeparator(), entity.getName());
		
		Object metaValue = entity.getScope().get(path.toString());
		metaValue = entity.getType().convert(metaValue);

		path.setLength(len);
		
		DependencyBean bean = entity.getMetaValues().get(metaValue);

		if (bean == null){
			throw new MappingException("bean not found: " + metaValue);
		}

		//metabean não tem nome
		this.updatePath(path, entity.getSeparator(), entity.getName());
		
		return this.getValue(bean, null, path, parent);
	}
	
	
	/* dependencyBean */
	
	public Object decode(DependencyBean dependencyBean, FetchType fetchType,
			Object data) throws BeanDecoderException {
		try{
			Map<String,Object> dta = new HashMap<String, Object>();
			
			return this.getValue(
				dependencyBean, fetchType, 
				(StringBuilder)dta.get("path"), 
				(NodeBeanDecoder)dta.get("parent"));
		}
		catch(Throwable e){
			throw new BeanDecoderException(e);
		}
	}

	private Object getValue(DependencyBean entity, FetchType fetchType, 
			StringBuilder path, NodeBeanDecoder parent) {
		
		if(fetchType == null){
			fetchType = entity.getFetchType();
		}
		
		if(fetchType.equals(FetchType.LAZY)){
			Map<String,Object> data = new HashMap<String, Object>();
			data.put("path",   new StringBuilder(path));
			data.put("parent", parent);
			
			ProxyFactory proxyFactory = 
					this.codeGenerator.getProxyFactory(entity.getClassType());
			return proxyFactory.getNewProxy(entity, data, this);
		}
		
		Object result;

		if (entity.getMapping() != null) {
			Bean dependencyBean = entity.getController().getBean(entity.getMapping());

			if (dependencyBean == null){
				throw new BrutosException("mapping not found: " + entity.getMapping());
			}

			Object value = this.getValue(dependencyBean, path, parent);
			
			return entity.getType().convert(value);
		}
		else
		if (entity.getMetaBean() == null) {
			if (entity.isStatic()){
				return entity.getValue();
			}
			else
			if(entity.isNullable()){
				return null;
			}
			else{
				
				Type type = entity.getType();
				
				result = 
					type instanceof CollectionType? 
						entity.getScope().getCollection(path.toString()) : 
						entity.getScope().get(path.toString());
						
				return entity.getType().convert(result);
			}

		}
		else{
			result = this.getValue(entity.getMetaBean(), path, parent);
			return entity.getType().convert(result);
		}
		
	}
	
	/* bean */
	
	private Object getValue(Bean entity, StringBuilder path, NodeBeanDecoder parent) {
		
		if(entity.isCollection()){
			return this.getValueCollection((CollectionBean)entity, path, parent);
		}
		else
		if(entity.isMap()){
			return this.getValueMap((MapBean)entity, path, parent);
		}
		else{
			return this.getValueBean(entity, path, parent);
		}
	}

	private Object getValueBean(Bean entity, StringBuilder path, NodeBeanDecoder parent) {
		
		ConstructorBean constructorBean = entity.getConstructor();
		Object value = this.getInstance(constructorBean, path, parent);
		
		if(value == null){
			return null;
		}
		
		Map<String, PropertyBean> props = entity.getFields();
		
		boolean exist =
				constructorBean.size() > 0 ||
				constructorBean.isMethodFactory() ||
				props.isEmpty();
		
		for(PropertyBean prop: props.values()){
			try{
				if(!prop.canSet()){
					continue;
				}
				
				int len = path.length();
				this.updatePath(path, prop.getParent().getSeparator(), prop.getParameterName());
				NodeBeanDecoder node = this.createNode(prop.getParameterName(), prop.getRealName());
				parent.addNode(prop.getRealName(), node);
				
				Object p = this.getValue(prop, null, path, node);
				
				if(p != null){
					exist = true;
					prop.setValueInSource(value, p);
				}
				
				path.setLength(len);
				
			}
			catch(Throwable ex){
				throw new DependencyException("fail parse property: " + prop.getName(), ex);
			}
		}
		
		return exist? value : null;
	}
	
	/* collection */
	
	private Object getValueCollection(CollectionBean entity, StringBuilder path, NodeBeanDecoder parent) {
		Element e = (Element)entity.getCollection();
		
		if(e.getParameterName() != null){
			return this.getValueCollectionObject(entity, e, path, parent);
		}
		else{
			return this.getValueCollectionSimple(entity, e, path, parent);
		}
	}

	@SuppressWarnings("unchecked")
	public Object getValueCollectionObject(CollectionBean entity, Element e,
			StringBuilder path, NodeBeanDecoder parent) {
		
		Collection<Object> destValue = (Collection<Object>)this.getValueBean(entity, path, parent);
	
		int len = path.length();
		this.updatePath(path, entity.getSeparator(), e.getParameterName());

		NodeBeanDecoder node = this.createNode(e.getParameterName(), null);
		parent.addNode(e.getParameterName(), node);
		
		int max = entity.getMaxItens() + 1;
		
		int lenEntity = path.length();
		
		for(int i=0;i<max;i++){
			
			path.append(entity.getIndexFormat().replace("$index", String.valueOf(i)));
			
			Object element = this.getValue(e, FetchType.EAGER, path, node);
			
			if(element != null){
				destValue.add(element);
			}
			else{
				break;
			}
			
			path.setLength(lenEntity);
		}
		
		if(destValue.size() > max){
			throw new DependencyException(destValue + " > " + max);
		}
		
		path.setLength(len);
		
		return destValue.isEmpty()? null : destValue;
	}
	
	@SuppressWarnings("unchecked")
	public Object getValueCollectionSimple(CollectionBean entity, Element e,
			StringBuilder path, NodeBeanDecoder parent) {
		
		Collection<Object> destValue = 
				(Collection<Object>)this.getValueBean(entity, path, parent);
	
		int len = path.length();

		int max = entity.getMaxItens() + 1;
		
		for(int i=0;i<max;i++){
			
			path.append(entity.getIndexFormat().replace("$index", String.valueOf(i)));
			
			Object element = this.getValue(e, FetchType.EAGER, path, parent);
			
			if(element != null){
				destValue.add(element);
			}
			else{
				break;
			}
			
			path.setLength(len);
			
		}
		
		if(destValue.size() > max){
			throw new DependencyException(destValue + " > " + max);
		}
		
		path.setLength(len);
		
		return destValue.isEmpty()? null : destValue;
	}	

	/* map */
	
	private Object getValueMap(MapBean entity, StringBuilder path, NodeBeanDecoder parent) {
		
		Key k = (Key)entity.getKey();
		
		if(k.getParameterName() != null){
			return this.getValueMapObject(entity, k, path, parent);
		}
		else{
			return this.getValueMapSimple(entity, k, path, parent);
		}
	}

	@SuppressWarnings("unchecked")
	public Object getValueMapObject(MapBean entity, Key k, StringBuilder path, NodeBeanDecoder parent){
		
		Map<Object,Object> destValue = 
				(Map<Object,Object>)this.getValueBean(entity, path, parent);

		int max = entity.getMaxItens() + 1;
		int len = path.length();
		
		List<Object> keys = new ArrayList<Object>();
		NodeBeanDecoder keyNode = this.createNode(k.getParameterName(), null);
		parent.addNode(k.getParameterName(), keyNode);
		
		this.updatePath(path, entity.getSeparator(), k.getParameterName());
		int keyLen = path.length();
		
		for(int i=0;i<max;i++){
			
			path.append(entity.getIndexFormat().replace("$index", String.valueOf(i)));
			
			Object key = this.getValue(k, FetchType.EAGER, path, keyNode);
			
			path.setLength(keyLen);
			keys.add(key);
		}

		path.setLength(len);
		
		Element e = (Element)entity.getCollection();
		
		NodeBeanDecoder eNode = this.createNode(e.getParameterName(), null);
		parent.addNode(e.getParameterName(), eNode);
		
		this.updatePath(path, entity.getSeparator(), k.getParameterName());
		int eLen = path.length();
		
		int i=0;
		
		for(Object key: keys){
			
			path.append(entity.getIndexFormat().replace("$index", String.valueOf(i)));
			
			Object element = this.getValue(e, FetchType.EAGER, path, keyNode);
			
			path.setLength(eLen);
			
			destValue.put(key, element);
		}
		
		if(destValue.size() > max){
			throw new DependencyException(destValue + " > " + max);
		}
		
		return destValue.isEmpty()? null : destValue;
	}

	@SuppressWarnings("unchecked")
	public Object getValueMapSimple(MapBean entity, Key k, StringBuilder path, NodeBeanDecoder parent){
		
		Map<Object,Object> destValue = 
				(Map<Object,Object>)this.getValueBean(entity, path, parent);

		int len = path.length();
		
		Element e = (Element)entity.getCollection();
		
		String prefix = path.toString();
		
		List<String> itens = 
				k.getScope()
					.getNamesStartsWith(prefix);

		if(itens.size() > entity.getMaxItens()){
			throw new DependencyException(itens.size() + " > " + entity.getMaxItens());
		}
		
		List<SimpleKeyMap> keys = 
				this.prepareKeysToSimpleMap(itens, prefix);
		
		for(SimpleKeyMap keyValue: keys){
			
			Object keyObject = k.convert(keyValue.getName());
			
			path.append(keyValue.getPrefix());
			
			Object element = this.getValue(e, FetchType.EAGER, path, parent);
			
			destValue.put(keyObject, element);
			
			path.setLength(len);
		}
		
		return destValue.isEmpty()? null : destValue;	
	}
	
	private boolean checkPrefix(String prefix, String key){
		int nextDot = key.indexOf(".", prefix.length());
		
		if(nextDot != -1){
			key = key.substring(0, nextDot);
		}
		
		return prefix.equals(key);
	}
	
	private List<SimpleKeyMap> prepareKeysToSimpleMap(List<String> itens, String prefix){
		
		List<SimpleKeyMap> result = new ArrayList<SimpleKeyMap>();
		 
		for(String item: itens){
			if(!this.checkPrefix(prefix, item)){
				continue;
			}
			String keyPrefix = item.substring(prefix.length());
			String key = keyPrefix;
			
			if(key.startsWith(".")){
				int endKeyName = key.indexOf(".", 1);
				
				if(endKeyName != -1){
					key = key.substring(1, endKeyName);
				}
				else{
					key = key.substring(1);
				}
			}
			else
			if(key.startsWith("[")){
				int endKeyName = key.indexOf("]");
				
				if(endKeyName != -1){
					key = key.substring(1, endKeyName - 1);
				}
				else{
					throw new MappingException("expected ']' in " + item);
				}
			}
			
			result.add(new SimpleKeyMap(key, keyPrefix));
		}
		
		return result;
	}	
	
	/* constructor */
	
	private Object getInstance(ConstructorBean constructor, StringBuilder path, NodeBeanDecoder parent){
		try{
			return constructor.isConstructor()? 
				this.getInstanceByConstructor(constructor, path, parent) :
				this.getInstanceByFactory(constructor, path, parent);
		}
		catch(Throwable e){
			throw new DependencyException("create instance failed: " + constructor.getBean().getName());
		}
	}
	
	private Object getInstanceByConstructor(ConstructorBean constructor,
			StringBuilder path, NodeBeanDecoder parent) throws InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		
		Constructor<?> insCons = constructor.getContructor();
		Object[] args          = this.getArgs(constructor, path, parent);
		
		if(args == null){
			return null;
		}
		
		return insCons.newInstance(args);
	}

	private Object getInstanceByFactory(ConstructorBean constructor, StringBuilder path, NodeBeanDecoder parent) 
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		
		String factory = constructor.getMethodFactory();
		Object factoryInstance;
		
		if(factory != null){
			Bean factoryBean = constructor.getBean().getController().getBean(factory);
			
			if(factoryBean == null){
				throw new IllegalStateException("bean factory not found: " + factory);
			}
			
			factoryInstance = this.getValue(factoryBean, path, parent);
			
		}
		else{
			factoryInstance = constructor.getBean().getClassType();
		}
		
		Method method = constructor.getMethod(factoryInstance);
		
		if (constructor.isCollection() && constructor.size() == 0)
			throw new MappingException("infinite loop detected: "
					+ constructor.getBean().getName());
		
		Object[] args  = this.getArgs(constructor, path, parent);
		
		return method.invoke(factoryInstance, args);
	}
	
	private Object[] getArgs(ConstructorBean constructor, StringBuilder path, NodeBeanDecoder parent) 
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{

		List<ConstructorArgBean> argsList = constructor.getConstructorArgs();
		
		Object[] args = new Object[argsList.size()];
		int i         = 0;
		boolean exist = argsList.size() < 1;
		
		for(ConstructorArgBean arg: constructor.getConstructorArgs()){
			
			int length = path.length();
			
			this.updatePath(path, arg.getParent().getSeparator(), arg.getParameterName());
			NodeBeanDecoder node = this.createNode(arg.getParameterName(), arg.getRealName());
			parent.addNode(arg.getRealName(), node);
			
			args[i] = this.getValue(arg, null, path, node);
			
			if(!exist){
				exist = exist || args[i] != null || arg.isNullable();
			}
			
			path.setLength(length);
			
			i++;
		}
		
		return exist? args : null;
	}

	/* util */

	public String getPerfixWithStartObject(String prefix, String separator, String name){
		if(StringUtil.isEmpty(prefix)){
			return name;
		}
		else
		if(!prefix.endsWith(separator)){
			return prefix + separator + name;
		}
		else{
			return prefix + name;
		}
	}

	public NodeBeanDecoder createNode(String path, String beanPath){
		NodeBeanDecoder node = new NodeBeanDecoder();
		node.setBeanPath(beanPath);
		node.setPath(path);
		return node;
	}
	
	public void updatePath(StringBuilder builder, String separator, String name){
		
		if(name != null){
			if(builder.length() == 0){
				builder.append(name);
			}
			else{
				builder.append(separator).append(name);
			}
		}
		
	}
	
}

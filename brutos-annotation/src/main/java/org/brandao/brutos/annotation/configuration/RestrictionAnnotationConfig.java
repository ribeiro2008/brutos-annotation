/*
 * Brutos Web MVC http://www.brutosframework.com.br/
 * Copyright (C) 2009 Afonso Brandao. (afonso.rbn@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brandao.brutos.annotation.configuration;

import org.brandao.brutos.ConfigurableApplicationContext;
import org.brandao.brutos.ParameterBuilder;
import org.brandao.brutos.PropertyBuilder;
import org.brandao.brutos.annotation.ActionParam;
import org.brandao.brutos.annotation.Stereotype;
import org.brandao.brutos.annotation.Property;
import org.brandao.brutos.annotation.Restriction;
import org.brandao.brutos.validator.RestrictionRules;

/**
 *
 * @author Brandao
 */
@Stereotype(target=Restriction.class,executeAfter={ActionParam.class,Property.class})
public class RestrictionAnnotationConfig extends AbstractAnnotationConfig{

    public boolean isApplicable(Object source) {
        return (source instanceof ActionParamEntry &&
                ((ActionParamEntry)source).isAnnotationPresent(Restriction.class)) ||
               (source instanceof PropertyEntry &&
                ((PropertyEntry)source).isAnnotationPresent(Restriction.class));
    }

    public Object applyConfiguration(Object source, Object builder, 
            ConfigurableApplicationContext applicationContext) {
        
        ParameterBuilder parameterBuilder = builder instanceof ParameterBuilder? (ParameterBuilder)builder : null;
        PropertyBuilder propertyBuilder = builder instanceof PropertyBuilder? (PropertyBuilder)builder : null;
        
        ActionParamEntry param = (ActionParamEntry)source;
        Restriction restriction = (Restriction)param.getAnnotation(Restriction.class);
        
        String rule = restriction.rule();
        String value = restriction.value();
        String message = restriction.message();
        
        if(parameterBuilder != null){
            parameterBuilder.addRestriction(RestrictionRules.valueOf(rule), value)
                .setMessage(message);
        }
        else{
            propertyBuilder.addRestriction(RestrictionRules.valueOf(rule), value)
                .setMessage(message);
        }
        
        return builder;
    }
    
}
/*
 * JBoss, Home of Professional Open Source Copyright 2010, Red Hat, Inc., and
 * individual contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.jboss.logging.model;

import java.util.Date;

import javax.annotation.Generated;

import org.jboss.logging.MessageLogger;

import com.sun.codemodel.internal.JAnnotationUse;
import com.sun.codemodel.internal.JBlock;
import com.sun.codemodel.internal.JClassAlreadyExistsException;
import com.sun.codemodel.internal.JCodeModel;
import com.sun.codemodel.internal.JDefinedClass;
import com.sun.codemodel.internal.JMethod;
import com.sun.codemodel.internal.JMod;

import org.jboss.logging.Logger;
import org.jboss.logging.ToolLogger;


/**
 * The java message logger java
 * class model.
 *
 * @author Kevin Pollet
 */
public class MessageLoggerClassModel extends ClassModel {

    /**
     * The logger parameter name.
     */
    private static final String LOGGER_PARAMETER_NAME = "logger";

    /**
     * Create a MessageBundle with super class and interface.
     *
     * @param className      the qualified class name
     * @param superClassName the super class name
     */
    public MessageLoggerClassModel(final ToolLogger logger, final String className, final String superClassName) {
        super(logger, className, superClassName);
    }

    public MessageLoggerClassModel(final ToolLogger logger, final String className, final String projectCode, final String superClassName, final String... interfacesName) {
        super(logger, className, projectCode, superClassName, interfacesName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JCodeModel generateModel() throws Exception {
        JCodeModel model = super.generateModel();
        JDefinedClass definedClass = model._getClass(this.getClassName());

        JMethod constructor = definedClass.constructor(JMod.PROTECTED);
        constructor.param(JMod.FINAL, Logger.class, LOGGER_PARAMETER_NAME);

        JBlock constructorBody = constructor.body();
        constructorBody.directStatement("super(" + LOGGER_PARAMETER_NAME + ");");

        return model;
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void initModel() throws JClassAlreadyExistsException {
       super.initModel();

        JCodeModel model = this.codeModel();

        /*
         * Add MessageLogger specific code
         */

        JDefinedClass definedClass = this.definedClass();

        //Add generated annotation
        JAnnotationUse generatedAnnotation = definedClass.annotate(Generated.class);
        generatedAnnotation.param("value", MessageLogger.class.getName());
        generatedAnnotation.param("date", new Date().toString());

        JMethod constructor = definedClass.constructor(JMod.PROTECTED);
        constructor.param(JMod.FINAL, Logger.class, LOGGER_PARAMETER_NAME);

        JBlock constructorBody = constructor.body();
        constructorBody.directStatement("super(" + LOGGER_PARAMETER_NAME + ");");
    }

    @Override
    protected void beforeWrite() {
        // TODO Auto-generated method stub
        
    }

}

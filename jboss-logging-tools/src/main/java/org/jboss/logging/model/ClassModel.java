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

import com.sun.codemodel.internal.JAnnotationUse;
import com.sun.codemodel.internal.JBlock;
import com.sun.codemodel.internal.JClass;
import com.sun.codemodel.internal.JClassAlreadyExistsException;
import com.sun.codemodel.internal.JCodeModel;
import com.sun.codemodel.internal.JDefinedClass;
import com.sun.codemodel.internal.JDocComment;
import com.sun.codemodel.internal.JExpr;
import com.sun.codemodel.internal.JFieldVar;
import com.sun.codemodel.internal.JMethod;
import com.sun.codemodel.internal.JMod;
import com.sun.codemodel.internal.JType;
import com.sun.codemodel.internal.JTypeVar;
import com.sun.codemodel.internal.JVar;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jboss.logging.validation.ValidationException;
import org.jboss.logging.validation.Validator;

/**
 * The basic java class model.
 *
 * @author Kevin Pollet
 * @author James R. Perkins Jr. (jrp)
 */
public abstract class ClassModel {

    /**
     * Qualified interface name.
     */
    private String[] interfaceNames;

    /**
     * Qualified super class name.
     */
    private String superClassName;

    /**
     * The class name.
     */
    private String className;

    /**
     * The corresponding model;
     */
    private JCodeModel codeModel;

    /**
     * Empty type array.
     */
    public static final JType[] EMPTY_TYPE_ARRAY = new JTypeVar[0];

    /**
     * The defined class.
     */
    private JDefinedClass definedClass;

    /**
     * The project code from the annotation.
     */
    private String projectCode;

    private final List<Validator> validators;

    /**
     * Construct a class model.
     *
     * @param className      the qualified class name
     * @param superClassName the qualified super class name
     */
    public ClassModel(final String className, final String superClassName) {
        this.interfaceNames = null;
        this.superClassName = superClassName;
        this.className = className;
        this.validators = new ArrayList<Validator>();
    }

    protected ClassModel(final String className,
            final String projectCode, final String superClassName,
            final String... interfaceNames) {
        this.interfaceNames = interfaceNames;
        this.superClassName = superClassName;
        this.className = className;
        this.projectCode = projectCode;
        this.validators = new ArrayList<Validator>();
    }

    /**
     * Creates the source file.
     *
     * <p>
     * Executes the following methods in the order listed.
     * <ol>
     * <li>Runs validation for each validator.</li>
     * <li>{@code ClassModel#generateModel()}</li>
     * </ol>
     * </p>
     *
     * @param fileObject the files object to write the source to.
     *
     * @throws Exception if an error occurs creating the source file.
     */
    public final void create(final JavaFileObject fileObject) throws IOException,
                                                                     IllegalStateException,
                                                                     ValidationException {
        for (Validator validator : validators) {
            validator.validate();
        }
        generateModel().build(new JavaFileObjectCodeWriter(fileObject));
    }

    /**
     * Generate the code corresponding to this
     * class model
     *
     * @return the generated code
     * @throws IllegalStateException if the class has already been defined.
     */
    protected JCodeModel generateModel() throws IllegalStateException {
        codeModel = new JCodeModel();
        try {
            definedClass = codeModel._class(this.className);
        } catch (JClassAlreadyExistsException e) {
            throw new IllegalStateException(
                    "Class " + this.className
                    + " has already been defined. Cannot generate the class.", e);
        }
        final JAnnotationUse anno = definedClass.annotate(
                javax.annotation.Generated.class);
        anno.param("value", getClass().getCanonicalName());
        anno.param("date", generatedDateValue());

        // Create the default JavaDoc
        final JDocComment docComment = definedClass.javadoc();
        docComment.add("Warning this class consists of generated code.");

        // Add extends
        if (this.superClassName != null) {
            definedClass._extends(codeModel.ref(this.superClassName));
        }

        // Add implements
        if (this.interfaceNames != null) {
            for (String intf : this.interfaceNames) {
                definedClass._implements(codeModel.ref(intf));
            }
        }
        return this.codeModel;
    }

    /**
     * Get the class model.
     *
     * @return the class model
     */
    protected final JCodeModel codeModel() {
        return this.codeModel;
    }

    /**
     * Returns the main enclosing class.
     *
     * @return the main enclosing class.
     */
    protected final JDefinedClass definedClass() {
        return definedClass;
    }

    /**
     * Adds a validator to be processed in the
     * {@code ClassModel#create(JavaFileObject)} after the
     * {@code ClassModel#initModel()}.
     *
     * @param validator the validator to add.
     */
    protected final void addValidator(final Validator validator) {
        this.validators.add(validator);
    }

    /**
     * Get the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Creates the variable that stores the message.
     * <p/>
     * <p>
     * If the message variable has already been defined the previously created
     * variable is returned.
     * </p>
     *
     * @param varName      the variable name.
     * @param messageValue the value for the message.
     * @return the newly created variable.
     */
    protected JVar addMessageVar(final String varName, final String messageValue) {
        JFieldVar var = definedClass().fields().get(varName);
        if (var == null) {
            var = definedClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
                    String.class, varName);
            var.init(JExpr.lit(messageValue));
        }
        return var;
    }

    /**
     * Adds a method to return the message value. The method name should be the
     * method name annotated {@code org.jboss.logging.Message}. This method will
     * be appended with {@code $str}.
     * <p/>
     * <p>
     * If the message method has already been defined the previously created
     * method is returned.
     * </p>
     * <p/>
     * <p>
     * Note this method invokes the
     * {@code addMessageVar(varName,messageValue,id)} to add the variable.
     * </p>
     *
     * @param methodName  the method name.
     * @param returnValue the message value.
     * @param id          the id to prepend the project code/message with.
     * @return the newly created method.
     */
    protected JMethod addMessageMethod(final String methodName,
            final String returnValue, final int id) {
        final String internalMethodName = methodName + "$str";
        JMethod method = definedClass().getMethod(internalMethodName,
                EMPTY_TYPE_ARRAY);
        // Create the method
        if (method == null) {
            final JClass returnType = codeModel().ref(String.class);
            method = definedClass().method(JMod.PROTECTED, returnType,
                    internalMethodName);
            final JBlock body = method.body();
            if (id > 0) {
                // Create the message id field
                final JVar idVar = definedClass.field(
                        JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
                        String.class, methodName + "Id");
                idVar.init(JExpr.lit(formatMessageId(id)));
                body._return(
                        idVar.plus(addMessageVar(methodName, returnValue)));
            } else {
                body._return(addMessageVar(methodName, returnValue));
            }
        }
        return method;
    }

    /**
     * Formats the message id. The message id is comprised of the project code
     * plus the id.
     *
     * @param id the id used to prepend the project code.
     * @return the formatted message id.
     */
    protected final String formatMessageId(final int id) {

        final StringBuilder result = new StringBuilder(projectCode);
        if (result.length() > 0) {
            result.append("-");
            result.append(padLeft("" + id, '0', 5));
            result.append(": ");
        }
        return result.toString();
    }

    /**
     * Pads the initial value with the character. If the length is greater than
     * or equal to the length of the initial value, the initial value will be
     * returned.
     *
     * @param initValue the value to pad.
     * @param padChar   the character to pad the value with.
     * @param padLen    the total length the string should be.
     * @return the padded value.
     */
    protected final String padLeft(final String initValue, final char padChar,
            final int padLen) {

        final StringBuilder result = new StringBuilder();
        for (int i = initValue.length(); i < padLen; i++) {
            result.append(padChar);
        }
        result.append(initValue);
        return result.toString();
    }

    /**
     * Returns the current date formatted in the ISO 8601 format.
     *
     * @return the current date formatted in ISO 8601.
     */
    protected static String generatedDateValue() {
        final SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssZ");
        return sdf.format(new Date());
    }
}

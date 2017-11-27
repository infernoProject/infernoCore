package ru.infernoproject.common.jmx;

import com.google.common.base.Joiner;
import ru.infernoproject.common.jmx.annotations.InfernoMBeanAttribute;
import ru.infernoproject.common.jmx.annotations.InfernoMBeanOperation;

import javax.management.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public interface InfernoMBean extends DynamicMBean {

    // Utility Methods

    default MBeanAttributeInfo[] getAttributesInfo() {
        return Arrays.stream(this.getClass().getFields())
            .filter(attribute -> attribute.isAnnotationPresent(InfernoMBeanAttribute.class))
            .map(attribute -> {
                InfernoMBeanAttribute attributeInfo = attribute.getAnnotation(InfernoMBeanAttribute.class);
                return new MBeanAttributeInfo(
                    attribute.getName(), attribute.getType().getCanonicalName(), attributeInfo.description(),
                    attributeInfo.isReadable(), attributeInfo.isWritable(), false
                );
            }).collect(Collectors.toList()).toArray(new MBeanAttributeInfo[] {});
    }

    default MBeanOperationInfo[] getOperationsInfo() {
        return Arrays.stream(this.getClass().getMethods())
            .filter(method -> method.isAnnotationPresent(InfernoMBeanOperation.class))
            .map(operation -> {
                InfernoMBeanOperation operationInfo = operation.getAnnotation(InfernoMBeanOperation.class);
                MBeanParameterInfo[] parameters = Arrays.stream(operation.getParameters())
                    .map(parameter -> new MBeanParameterInfo(parameter.getName(), parameter.getType().getSimpleName(), ""))
                    .collect(Collectors.toList()).toArray(new MBeanParameterInfo[] {});

                return new MBeanOperationInfo(
                    operation.getName(), operationInfo.description(), parameters,
                    operation.getReturnType().getSimpleName(), operationInfo.impact()
                );
            }).collect(Collectors.toList()).toArray(new MBeanOperationInfo[] {});
    }

    default MBeanConstructorInfo[] getConstructorsInfo() {
        return Arrays.stream(this.getClass().getConstructors())
            .map(constructor -> new MBeanConstructorInfo("", constructor))
            .collect(Collectors.toList()).toArray(new MBeanConstructorInfo[] {});
    }

    default boolean checkSignature(Method method, String[] signature) {
        if (signature.length != method.getParameterCount())
            return false;

        Class<?>[] methodSignature = method.getParameterTypes();
        for (int parameter = 0; parameter < signature.length; parameter++) {
            if (!methodSignature[parameter].getCanonicalName().equals(signature[parameter]))
                return false;
        }

        return true;
    }

    default Method getMethod(String name, String[] signature) throws NoSuchMethodException {
        Optional<Method> methodObject = Arrays.stream(this.getClass().getMethods())
            .filter(method -> method.getName().equals(name))
            .filter(method -> checkSignature(method, signature))
            .findFirst();

        if (!methodObject.isPresent())
            throw new NoSuchMethodException(this.getClass().getName() + "." + name + "(" + Joiner.on(", ").join(signature) + ")");

        return methodObject.get();
    }

    // MBean Methods

    @Override
    default Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        try {
            Field attributeField = this.getClass().getField(attribute);
            if (!attributeField.isAnnotationPresent(InfernoMBeanAttribute.class)) {
                throw new AttributeNotFoundException(
                    String.format("Attribute '%s' of class %s is not registered as MBean attribute", attribute, this.getClass().getSimpleName())
                );
            }

            return attributeField.get(this);
        } catch (NoSuchFieldException e) {
            throw new AttributeNotFoundException(String.format("%s has no attribute '%s'", this.getClass().getSimpleName(), attribute));
        } catch (IllegalAccessException e) {
            throw new MBeanException(e);
        }
    }

    @Override
    default void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        try {
            Field attributeField = this.getClass().getField(attribute.getName());
            if (!attributeField.isAnnotationPresent(InfernoMBeanAttribute.class)) {
                throw new AttributeNotFoundException(
                    String.format("Attribute '%s' of class %s is not registered as MBean attribute", attribute.getName(), this.getClass().getSimpleName())
                );
            }

            attributeField.set(this, attribute.getValue());
        } catch (NoSuchFieldException e) {
            throw new AttributeNotFoundException(String.format("%s has no attribute '%s'", this.getClass().getSimpleName(), attribute));
        } catch (IllegalAccessException e) {
            throw new MBeanException(e);
        }
    }

    @Override
    default AttributeList getAttributes(String[] attributes) {
        return new AttributeList(Arrays.stream(attributes).map(attribute -> {
            try {
                return new Attribute(attribute, getAttribute(attribute));
            } catch (AttributeNotFoundException | MBeanException | ReflectionException e) {
                return null;
            }
        }).collect(Collectors.toList()));
    }

    @Override
    default AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList(attributes.asList().stream().map(attribute -> {
            try {
                setAttribute(attribute);

                return attribute;
            } catch (AttributeNotFoundException | InvalidAttributeValueException | ReflectionException | MBeanException e) {
                return new Attribute(attribute.getName(), null);
            }
        }).collect(Collectors.toList()));
    }

    @Override
    default Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        try {
            Method actionMethod = getMethod(actionName, signature);

            if (!actionMethod.isAnnotationPresent(InfernoMBeanOperation.class)) {
                throw new IllegalArgumentException(
                    String.format("Method '%s' of class %s is not registered as MBean operation", actionName, this.getClass().getSimpleName())
                );
            }

            return actionMethod.invoke(this, params);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new MBeanException(e);
        }
    }

    @Override
    default MBeanInfo getMBeanInfo() {
        MBeanAttributeInfo[] attributes = getAttributesInfo();
        MBeanConstructorInfo[] constructors = getConstructorsInfo();
        MBeanOperationInfo[] operations = getOperationsInfo();
        MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[0];

        return new MBeanInfo(
            this.getClass().getSimpleName(), "",
            attributes, constructors,
            operations, notifications
        );
    }
}

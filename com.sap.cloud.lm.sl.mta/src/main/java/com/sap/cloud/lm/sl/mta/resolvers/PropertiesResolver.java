package com.sap.cloud.lm.sl.mta.resolvers;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.helpers.SimplePropertyVisitor;
import com.sap.cloud.lm.sl.mta.helpers.VisitableObject;
import com.sap.cloud.lm.sl.mta.message.Messages;

public class PropertiesResolver implements SimplePropertyVisitor, Resolver<Map<String, Object>> {

    private Map<String, Object> properties;
    private String prefix;
    private ProvidedValuesResolver valuesResolver;
    private ReferencePattern referencePattern;
    private boolean isStrict;
    private ResolutionContext resolutionContext;

    public PropertiesResolver() {
        // do nothing
    }

    public PropertiesResolver(Map<String, Object> properties, ProvidedValuesResolver valuesResolver, ReferencePattern referencePattern,
        String prefix) {
        this(properties, valuesResolver, referencePattern, prefix, true);
    }

    public PropertiesResolver(Map<String, Object> properties, ProvidedValuesResolver valuesResolver, ReferencePattern referencePattern,
        String prefix, boolean isStrict) {
        this.properties = properties;
        this.prefix = prefix;
        this.referencePattern = referencePattern;
        this.isStrict = isStrict;
        this.valuesResolver = valuesResolver;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> resolve() {
        return (Map<String, Object>) resolve(properties);
    }

    private Object resolve(Object value) {
        return new VisitableObject(value).accept(this);
    }

    private Object resolve(String key, Object value) {
        return new VisitableObject(value).accept(key, this);
    }

    @Override
    public Object visit(String key, String value) {
        if (resolutionContext != null) {
            resolutionContext.addToReferencedKeys(key);
        }
        return resolveReferences(key, value);
    }

    private Object resolveReferences(String key, String value) {
        List<Reference> references = detectReferences(value);
        if (isSimpleReference(value, references)) {
            return resolveReferenceInContext(key, references.get(0));
        }
        StringBuilder result = new StringBuilder(value);
        for (Reference reference : references) {
            result = resolveReferenceInPlace(key, result, reference);
            if (result == null) {
                return null;
            }
        }
        return result.toString();
    }

    private StringBuilder resolveReferenceInPlace(String key, StringBuilder value, Reference reference) {
        String matchedPattern = reference.getMatchedPattern();
        int patternStartIndex = value.indexOf(matchedPattern);
        Object resolvedReference = resolveReferenceInContext(key, reference);
        if (resolvedReference != null) {
            return value.replace(patternStartIndex, patternStartIndex + matchedPattern.length(), resolvedReference.toString());
        }
        return null;
    }

    private boolean isSimpleReference(String value, List<Reference> references) {
        return references.size() == 1 && value.length() == references.get(0)
            .getMatchedPattern()
            .length();
    }

    protected Object resolveReferenceInContext(String key, Reference reference) {
        boolean resolutionContextWasCreated = false;
        if (resolutionContext == null) {
            resolutionContext = new ResolutionContext(getPrefixedName(prefix, key));
            resolutionContextWasCreated = true;
        }
        Object resolvedValue = resolveReference(reference);
        if (resolutionContextWasCreated) {
            resolutionContext = null;
        }
        return resolvedValue;
    }

    private Object resolveReference(Reference reference) {
        String referencedPropertyKey = reference.getPropertyName();
        Map<String, Object> replacementValues = valuesResolver.resolveProvidedValues(reference.getDependencyName());

        boolean canResolveInDepth = referencePattern.hasDepthOfReference() && referencedPropertyKey.contains("/");
        if (replacementValues == null || (!replacementValues.containsKey(referencedPropertyKey) && !canResolveInDepth)) {            
            if (isStrict) {
                throw new ContentException(Messages.UNABLE_TO_RESOLVE, getPrefixedName(prefix, referencedPropertyKey));
            }
            return null;
        }
        // always try to resolve as a flat reference first
        Object referencedProperty = replacementValues.get(referencedPropertyKey);
        
        if (referencedProperty == null && canResolveInDepth) {
            referencedProperty = resolveInDepth(replacementValues, referencedPropertyKey);
        }
        
        String referencedPropertyKeyWithSuffix = getReferencedPropertyKeyWithSuffix(reference);
        return resolve(referencedPropertyKeyWithSuffix, referencedProperty);
    }
    
    protected Object resolveInDepth(Map<String, Object> properties, String propertyKey) {
        Matcher matcher = Pattern.compile("([^/]+)/?").matcher(propertyKey);
        
        if (!matcher.find()) {
            if (isStrict) {
                throw new ContentException(Messages.UNABLE_TO_RESOLVE, getPrefixedName(prefix, propertyKey));
            }
            return null;
        }
        
        Object currentProperty = properties.get(matcher.group(1));
        
        while(matcher.find()) {
            String subKey = matcher.group(1);
            
            if (StringUtils.isNumeric(subKey)) {
                if (currentProperty instanceof Collection) {
                    try {
                        currentProperty = IterableUtils.get((Collection<?>)currentProperty, Integer.parseInt(subKey));
                        continue;
                    } catch (IndexOutOfBoundsException e) {}
                }
                throw new ContentException(Messages.UNABLE_TO_RESOLVE, getPrefixedName(prefix, propertyKey));
            } else {
                if (currentProperty instanceof Map) {
                    currentProperty = MapUtils.getObject((Map<String, ?>) currentProperty, subKey);
                    if (currentProperty != null) {
                        continue;
                    }
                }
                
                throw new ContentException(Messages.UNABLE_TO_RESOLVE, getPrefixedName(prefix, propertyKey));
            }
        }
        
        return currentProperty;
    }
    
    private String getReferencedPropertyKeyWithSuffix(Reference reference) {
        if (reference.getDependencyName() != null) {
            return getPrefixedName(reference.getDependencyName(), reference.getPropertyName());
        }
        return reference.getPropertyName();
    }

    private List<Reference> detectReferences(String line) {
        return referencePattern.match(line);
    }

    private static class ResolutionContext {

        private String rootKey;
        private Set<String> referencedKeys = new HashSet<>();

        public ResolutionContext(String rootKey) {
            this.rootKey = rootKey;
        }

        public void addToReferencedKeys(String key) {
            if (referencedKeys.contains(key)) {
                throw new ContentException(Messages.DETECTED_CIRCULAR_REFERENCE, rootKey);
            }
            referencedKeys.add(key);
        }

    }

}
package alien4cloud.suggestions.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.google.common.io.Closeables;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.SuggestionEntry;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.utils.YamlParserUtil;

import com.google.common.collect.Maps;

@Slf4j
@Component
public class SuggestionService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO elasticSearchDAO;

    @PostConstruct
    public void loadDefaultSuggestions() throws IOException {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("suggestion-configuration.yml");
        SuggestionEntry[] suggestions = YamlParserUtil.parse(input, SuggestionEntry[].class);
        Closeables.close(input, true);
        for (SuggestionEntry suggestionEntry : suggestions) {
            elasticSearchDAO.save(suggestionEntry);
        }
    }

    public Set<String> getSuggestions(String suggestionId) {
        SuggestionEntry suggestionEntry = elasticSearchDAO.findById(SuggestionEntry.class, suggestionId);
        if (suggestionEntry == null) {
            throw new NotFoundException("Suggestion entry [" + suggestionId + "] cannot be found");
        } else {
            return suggestionEntry.getSuggestions();
        }
    }

    public void createSuggestionEntry(Class<? extends IndexedInheritableToscaElement> targetClass, String targetId, String targetProperty,
            Set<String> initialValues) {
        String esIndex = elasticSearchDAO.getIndexForType(targetClass);
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("elementId", new String[] { targetId });

        GetMultipleDataResult<? extends IndexedInheritableToscaElement> result = elasticSearchDAO.find(targetClass, filters, Integer.MAX_VALUE);
        if (result.getData() != null && result.getData().length > 0) {
            for (IndexedInheritableToscaElement targetElement : result.getData()) {
                PropertyDefinition propertyDefinition = targetElement.getProperties().get(targetProperty);
                if (propertyDefinition == null) {
                    throw new NotFoundException("Property [" + targetProperty + "] not found for element [" + targetId + "]");
                } else {
                    SuggestionEntry suggestionEntry = new SuggestionEntry();
                    suggestionEntry.setEsIndex(esIndex);
                    suggestionEntry.setEsType(MappingBuilder.indexTypeFromClass(targetElement.getClass()));
                    suggestionEntry.setTargetElementId(targetId);
                    suggestionEntry.setTargetProperty(targetProperty);
                    suggestionEntry.setSuggestions(initialValues);
                    switch (propertyDefinition.getType()) {
                    case ToscaType.STRING:
                        propertyDefinition.setSuggestionId(suggestionEntry.getId());
                        elasticSearchDAO.save(suggestionEntry);
                        elasticSearchDAO.save(targetElement);
                        break;
                    case ToscaType.LIST:
                    case ToscaType.MAP:
                        PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
                        if (entrySchema != null) {
                            entrySchema.setSuggestionId(suggestionEntry.getId());
                            elasticSearchDAO.save(suggestionEntry);
                            elasticSearchDAO.save(targetElement);
                        } else {
                            throw new InvalidArgumentException("Cannot suggest a list / map type with no entry schema definition");
                        }
                        break;
                    default:
                        throw new InvalidArgumentException(propertyDefinition.getType()
                                + " cannot be suggested, only property of type string list or map can be suggested");
                    }
                }
            }
        } else {
            throw new NotFoundException("Not any target element found for suggestion from [" + targetClass.getName() + "] and id [" + targetId + "]");
        }
    }
}

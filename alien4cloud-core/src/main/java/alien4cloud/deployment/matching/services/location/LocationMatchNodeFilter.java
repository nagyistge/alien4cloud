package alien4cloud.deployment.matching.services.location;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.Getter;

import org.springframework.stereotype.Component;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.orchestrators.ArtifactSupport;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.services.OrchestratorService;

import com.google.common.collect.Maps;

@Component
public class LocationMatchNodeFilter extends AbstractLocationMatchFilterWithElector {
    @Resource
    private CSARRepositorySearchService csarSearchService;
    @Resource
    private OrchestratorService orchestratorService;
    @Inject
    private LocationMatchNodesArtifactsElector artifactsElector;

    @Override
    public void filter(List<ILocationMatch> toFilter, Topology topology) {

        // create a context to keep requested tosca elements.
        NodeMatchContext nodeMatchContext = new NodeMatchContext();
        nodeMatchContext.topology = topology;

        for (Entry<String, NodeTemplate> entry : topology.getNodeTemplates().entrySet()) {
            nodeMatchContext.template = entry.getValue();
            for (Iterator<ILocationMatch> it = toFilter.iterator(); it.hasNext();) {
                nodeMatchContext.locationMatch = it.next();
                if (!artifactsElector.isEligible(nodeMatchContext)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Context to match a node against a location.
     */
    @Getter
    public class NodeMatchContext {
        private Map<String, Map<String, IndexedToscaElement>> toscaTypesCache = Maps.newHashMap();
        private Topology topology;
        private NodeTemplate template;
        private ILocationMatch locationMatch;

        /**
         * Get an element from the local-cache or from ES.
         * 
         * @param elementClass The class of the element to look for.
         * @param elementId The id of the element to look for.
         * @param <T> The type of element.
         * @return The requested element.
         */
        public <T extends IndexedToscaElement> T getElement(Class<T> elementClass, String elementId) {
            String elementType = elementClass.getSimpleName();
            Map<String, IndexedToscaElement> typeElements = toscaTypesCache.get(elementType);
            if (typeElements == null) {
                typeElements = new HashMap<>();
                toscaTypesCache.put(elementType, typeElements);
            } else {
                // find in local-cache
                T element = (T) typeElements.get(elementId);
                if (element != null) {
                    return element;
                }
            }

            T element = csarSearchService.getRequiredElementInDependencies(elementClass, elementId, topology.getDependencies());
            typeElements.put(elementId, element);
            return element;
        }

        public ArtifactSupport getArtifactSupport() {
            IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(locationMatch.getOrchestrator());
            return orchestratorFactory.getArtifactSupport();
        }
    }
}
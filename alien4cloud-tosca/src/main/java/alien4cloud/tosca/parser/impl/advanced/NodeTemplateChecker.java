package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NodeTemplateChecker implements IChecker<NodeTemplate> {


    private static final String KEY = "nodeTemplateChecker";

    @Resource
    private ICSARRepositorySearchService searchService;

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void before(ParsingContextExecution context, Node node) {
    }

    @Override
    public void check(NodeTemplate instance, ParsingContextExecution context, Node node) {
        String nodeTypeName = instance.getType();
        final ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        IndexedNodeType indexedNodeType = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(nodeTypeName, archiveRoot, searchService);
        if (indexedNodeType == null) {
            // node type can't be found neither in archive or in dependencies
            context.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Derived_from type not found", node.getStartMark(),
                    "The type specified for a node_template is not found neither in the archive nor its dependencies.", node.getEndMark(), nodeTypeName));
            return;
        }

        NodeTemplate tempObject = NodeTemplateBuilder.buildNodeTemplate(indexedNodeType, instance);

        instance.setAttributes(tempObject.getAttributes());
        instance.setCapabilities(tempObject.getCapabilities());
        instance.setProperties(tempObject.getProperties());
        instance.setRequirements(tempObject.getRequirements());
        instance.setArtifacts(tempObject.getArtifacts());
        instance.setInterfaces(tempObject.getInterfaces());
    }
}
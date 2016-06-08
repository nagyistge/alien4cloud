package alien4cloud.tosca.parser;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.model.components.*;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;

@Component
public class TemplatePostProcessor {
    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     *
     * @param parsedArchive The archive to post process
     */
    public ParsingResult<ArchiveRoot> process(ParsingResult<ArchiveRoot> parsedArchive) {
        Map<String, String> globalElementsMap = Maps.newHashMap();
        postProcessArchive(parsedArchive.getResult().getArchive().getName(), parsedArchive.getResult().getArchive().getVersion(), parsedArchive,
                globalElementsMap);
        return parsedArchive;
    }

    private final void postProcessArchive(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, String> globalElementsMap) {
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getNodeTypes(), globalElementsMap);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getNodeTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getRelationshipTypes(), globalElementsMap);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getRelationshipTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getCapabilityTypes(), globalElementsMap);
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getArtifactTypes(), globalElementsMap);
        postProcessTopology(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getTopology(), globalElementsMap);
    }

    private void postProcessTopology(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, Topology topology,
            Map<String, String> globalElementsMap) {
        if (topology == null) {
            return;
        }
        for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
            postProcessNodeTemplate(archiveName, archiveVersion, parsedArchive, nodeTemplate, globalElementsMap);
        }
    }

    private void postProcessNodeTemplate(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, NodeTemplate nodeTemplate,
            Map<String, String> globalElementsMap) {
        postProcessInterfaces(parsedArchive.getResult(), nodeTemplate.getInterfaces());
        if (nodeTemplate.getRelationships() != null) {
            for (RelationshipTemplate relationship : nodeTemplate.getRelationships().values()) {
                postProcessInterfaces(parsedArchive.getResult(), relationship.getInterfaces());
            }
        }
    }

    private final void postProcessElements(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, ? extends IndexedInheritableToscaElement> elements, Map<String, String> globalElementsMap) {
        if (elements == null) {
            return;
        }
        for (Map.Entry<String, ? extends IndexedInheritableToscaElement> element : elements.entrySet()) {
            element.getValue().setId(element.getKey());
            element.getValue().setArchiveName(archiveName);
            element.getValue().setArchiveVersion(archiveVersion);
            String previous = globalElementsMap.put(element.getKey(), parsedArchive.getContext().getFileName());
            if (previous != null) {
                parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.DUPLICATED_ELEMENT_DECLARATION,
                        "Type is defined twice in archive.", null, parsedArchive.getContext().getFileName(), null, previous));
            }
        }
    }

    private void postProcessIndexedArtifactToscaElement(ArchiveRoot archive, Map<String, ? extends IndexedArtifactToscaElement> elements) {
        if (elements == null) {
            return;
        }
        for (IndexedArtifactToscaElement element : elements.values()) {
            postProcessDeploymentArtifacts(archive, element);
            postProcessInterfaces(archive, element.getInterfaces());
        }
    }

    private void postProcessDeploymentArtifacts(ArchiveRoot archive, IndexedArtifactToscaElement element) {
        if (element.getArtifacts() == null) {
            return;
        }

        for (DeploymentArtifact artifact : element.getArtifacts().values()) {
            postProcessDeploymentArtifact(archive, artifact);
        }
    }

    private void postProcessInterfaces(ArchiveRoot archive, Map<String, Interface> interfaces) {
        if (interfaces == null) {
            return;
        }

        for (Interface interfaz : interfaces.values()) {
            for (Operation operation : interfaz.getOperations().values()) {
                postProcessImplementationArtifact(archive, operation.getImplementationArtifact());
            }
        }
    }

    private void postProcessDeploymentArtifact(ArchiveRoot archive, DeploymentArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }

    private void postProcessImplementationArtifact(ArchiveRoot archive, ImplementationArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }
}
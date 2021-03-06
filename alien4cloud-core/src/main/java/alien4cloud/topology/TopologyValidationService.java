package alien4cloud.topology;

import alien4cloud.application.ApplicationService;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.common.TagService;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.NodeFiltersTask;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.RequirementsTask;
import alien4cloud.topology.task.SuggestionsTask;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.topology.task.WorkflowTask;
import alien4cloud.topology.validation.NodeFilterValidationService;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;
import alien4cloud.topology.validation.TopologyAbstractRelationshipValidationService;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.topology.validation.TopologyRequirementBoundsValidationServices;
import alien4cloud.utils.services.ConstraintPropertyService;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TopologyValidationService {
    @Resource
    private MetaPropertiesService metaPropertiesService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private TagService tagService;
    @Resource
    private ConstraintPropertyService constraintPropertyService;

    @Resource
    private TopologyPropertiesValidationService topologyPropertiesValidationService;
    @Resource
    private TopologyRequirementBoundsValidationServices topologyRequirementBoundsValidationServices;
    @Resource
    private TopologyAbstractRelationshipValidationService topologyAbstractRelationshipValidationService;
    @Resource
    private TopologyAbstractNodeValidationService topologyAbstractNodeValidationService;
    @Resource
    private NodeFilterValidationService nodeFilterValidationService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    /**
     * Validate if a topology is valid for deployment configuration or not,
     * This is done before deployment configuration
     *
     * @param topology topology to be validated
     * @return the validation result
     */
    public TopologyValidationResult validateTopology(Topology topology) {
        TopologyValidationResult dto = new TopologyValidationResult();
        if (topology.getNodeTemplates() == null || topology.getNodeTemplates().size() < 1) {
            dto.setValid(false);
            return dto;
        }

        // validate the workflows
        dto.addTasks(workflowBuilderService.validateWorkflows(topology));

        // validate abstract relationships
        dto.addTasks(topologyAbstractRelationshipValidationService.validateAbstractRelationships(topology));

        // validate abstract node types and find suggestions
        // in this step, this is a warning, since they can be replaced by nodes comming from the location
        // TODO should we do this here or not?
        // dto.addToWarningList(topologyAbstractNodeValidationService.findReplacementForAbstracts(topology));

        // validate requirements lowerBounds
        dto.addTasks(topologyRequirementBoundsValidationServices.validateRequirementsLowerBounds(topology));

        // validate the node filters for all relationships
        dto.addTasks(nodeFilterValidationService.validateStaticRequirementFilters(topology));

        // validate required properties (properties of NodeTemplate, Relationship and Capability)
        List<PropertiesTask> validateProperties = topologyPropertiesValidationService.validateStaticProperties(topology);

        // List<PropertiesTask> validateProperties = null;
        if (hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addWarnings(validateProperties);
        } else {
            dto.addTasks(validateProperties);
        }

        dto.setValid(isValidTaskList(dto.getTaskList()));

        return dto;
    }

    public static boolean hasOnlyPropertiesWarnings(List<PropertiesTask> properties) {
        if (properties == null) {
            return true;
        }
        for (PropertiesTask task : properties) {
            if (CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED))
                    || CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.ERROR))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Define if a tasks list is valid or not regarding task types
     *
     * @param taskList
     * @return
     */
    public static boolean isValidTaskList(List<AbstractTask> taskList) {
        if (taskList == null) {
            return true;
        }
        for (AbstractTask task : taskList) {
            // checking some required tasks
            if (task instanceof SuggestionsTask || task instanceof RequirementsTask || task instanceof PropertiesTask || task instanceof NodeFiltersTask
                    || task instanceof WorkflowTask) {
                return false;
            }
        }
        return true;
    }
}

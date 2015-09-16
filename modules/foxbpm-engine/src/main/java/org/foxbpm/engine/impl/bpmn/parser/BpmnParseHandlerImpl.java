/**
 * Copyright 1996-2014 FoxBPM ORG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author kenshin
 */
package org.foxbpm.engine.impl.bpmn.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.foxbpm.bpmn.converter.BpmnXMLConverter;
import org.foxbpm.engine.event.EventListener;
import org.foxbpm.engine.exception.FoxBPMException;
import org.foxbpm.engine.impl.Context;
import org.foxbpm.engine.impl.ProcessDefinitionEntityBuilder;
import org.foxbpm.engine.impl.bpmn.behavior.ActivityBehavior;
import org.foxbpm.engine.impl.bpmn.behavior.BaseElementBehavior;
import org.foxbpm.engine.impl.bpmn.behavior.BoundaryEventBehavior;
import org.foxbpm.engine.impl.bpmn.behavior.EventBehavior;
import org.foxbpm.engine.impl.bpmn.behavior.FlowNodeBehavior;
import org.foxbpm.engine.impl.bpmn.behavior.SequenceFlowBehavior;
import org.foxbpm.engine.impl.bpmn.behavior.SubProcessBehavior;
import org.foxbpm.engine.impl.connector.ConnectorListener;
import org.foxbpm.engine.impl.entity.ProcessDefinitionEntity;
import org.foxbpm.engine.impl.mgmt.DataVariableMgmtDefinition;
import org.foxbpm.engine.impl.util.ExceptionUtil;
import org.foxbpm.engine.modelparse.ProcessModelParseHandler;
import org.foxbpm.kernel.ProcessDefinitionBuilder;
import org.foxbpm.kernel.behavior.KernelFlowNodeBehavior;
import org.foxbpm.kernel.behavior.KernelSequenceFlowBehavior;
import org.foxbpm.kernel.event.KernelEventType;
import org.foxbpm.kernel.event.KernelListener;
import org.foxbpm.kernel.process.KernelDIBounds;
import org.foxbpm.kernel.process.KernelFlowElementsContainer;
import org.foxbpm.kernel.process.KernelLane;
import org.foxbpm.kernel.process.KernelLaneSet;
import org.foxbpm.kernel.process.KernelProcessDefinition;
import org.foxbpm.kernel.process.impl.KernelArtifactImpl;
import org.foxbpm.kernel.process.impl.KernelAssociationImpl;
import org.foxbpm.kernel.process.impl.KernelBaseElementImpl;
import org.foxbpm.kernel.process.impl.KernelFlowElementsContainerImpl;
import org.foxbpm.kernel.process.impl.KernelFlowNodeImpl;
import org.foxbpm.kernel.process.impl.KernelLaneImpl;
import org.foxbpm.kernel.process.impl.KernelLaneSetImpl;
import org.foxbpm.kernel.process.impl.KernelSequenceFlowImpl;
import org.foxbpm.model.Activity;
import org.foxbpm.model.BaseElement;
import org.foxbpm.model.Bounds;
import org.foxbpm.model.BpmnModel;
import org.foxbpm.model.Connector;
import org.foxbpm.model.FlowElement;
import org.foxbpm.model.FlowNode;
import org.foxbpm.model.Lane;
import org.foxbpm.model.LaneSet;
import org.foxbpm.model.Process;
import org.foxbpm.model.SequenceFlow;
import org.foxbpm.model.StartEvent;
import org.foxbpm.model.SubProcess;
import org.foxbpm.model.WayPoint;
import org.foxbpm.model.constant.StyleOption;
import org.foxbpm.model.style.Style;

public class BpmnParseHandlerImpl implements ProcessModelParseHandler {
	
	private static Map<String, Style> styleContainer = new HashMap<String, Style>(); 
	//初始化加载监听器
	private static Map<String,List<String>> eventTypeMap = new HashMap<String,List<String>>();
	private static final String PROCESSDEFINITION_EVENT = "ProcessDefinitionEvent";
	private static final String SEQUENCEFLOW_EVENT = "KernelSequenceFlowImplEvent";
	private static final String FLOWNODE_EVENT = "KernelFlowNodeImplEvent";
	static{
		//初始化流程定义事件监听
		List<String> processDefinitionEventList = new ArrayList<String>();
		processDefinitionEventList.add(KernelEventType.EVENTTYPE_PROCESS_START);
		processDefinitionEventList.add(KernelEventType.EVENTTYPE_PROCESS_END);
		processDefinitionEventList.add(KernelEventType.EVENTTYPE_PROCESS_ABORT);
//		processDefinitionEventList.add(KernelEventType.EVENTTYPE_BEFORE_PROCESS_SAVE);
		eventTypeMap.put(PROCESSDEFINITION_EVENT, processDefinitionEventList);
		
		//初始化流程节点事件监听
		List<String> flowNodeEventList = new ArrayList<String>();
		flowNodeEventList.add(KernelEventType.EVENTTYPE_NODE_ENTER);
		flowNodeEventList.add(KernelEventType.EVENTTYPE_NODE_EXECUTE);
		flowNodeEventList.add(KernelEventType.EVENTTYPE_NODE_LEAVE); 
		eventTypeMap.put(FLOWNODE_EVENT, flowNodeEventList);
		
		//初始化线条事件监听
		List<String> sequenceFlowEventList = new ArrayList<String>();
		sequenceFlowEventList.add(KernelEventType.EVENTTYPE_SEQUENCEFLOW_TAKE);
		eventTypeMap.put(SEQUENCEFLOW_EVENT, sequenceFlowEventList);
		
	} 
	
	@SuppressWarnings("unchecked")
	public BpmnParseHandlerImpl() {
		SAXReader reader = new SAXReader();
		try {
			Document doc = reader.read(this.getClass().getClassLoader().getResourceAsStream("config/style.xml"));
			Element element = doc.getRootElement();
			Element configElement = element.element("elementStyleConfig");
			String currentStyle = configElement.attributeValue("currentStyle");
			Iterator<Element> elemIterator = configElement.elements("elementStyle").iterator();
			Element styleConfigElemnt = null;
			while (elemIterator.hasNext()) {
				styleConfigElemnt = elemIterator.next();
				String styleId = styleConfigElemnt.attributeValue("styleId");
				if (currentStyle.equals(styleId)) {
					break;
				}
			}
			if (styleConfigElemnt == null) {
				throw new RuntimeException("style.xml文件格式不正确，请检查！");
			}
			Iterator<Element> styleElementIterator = styleConfigElemnt.elements("style").iterator();
			while (styleElementIterator.hasNext()) {
				Element styleElement = styleElementIterator.next();
				Style tmpStyle = new Style();
				String elementType = styleElement.attributeValue("object");
				tmpStyle.setElementType(elementType);
				tmpStyle.setBackGround(styleElement.attributeValue("background"));
				tmpStyle.setForeGround(styleElement.attributeValue("foreground"));
				tmpStyle.setSelectedColor(styleElement.attributeValue("selectedColor"));
				tmpStyle.setMulitSelectedColor(styleElement.attributeValue("selectedColor"));
				tmpStyle.setTextColor(styleElement.attributeValue("textColor"));
				tmpStyle.setFont(styleElement.attributeValue("font"));
				styleContainer.put(elementType, tmpStyle);
			}
		} catch (DocumentException e) {
			throw ExceptionUtil.getException("10106000", e);
		}
	}
	
	public static BehaviorRelationMemo behaviorRelationMemo = new BehaviorRelationMemo();
	
	public KernelProcessDefinition createProcessDefinition(String processId, Object processFile) {
		BpmnModel bpmnModel = null;
		bpmnModel = loadBpmnModel(processId, (InputStream) processFile);
		if (bpmnModel == null) {
			throw new FoxBPMException("文件中没有对应的流程定义，请检查bpmn文件内容和流程key是否对应！");
		}
		KernelProcessDefinition processDefinition = loadProcess(bpmnModel);
		// 关联保存下来的关系
		behaviorRelationMemo.attachActivityAndBoundaryEventBehaviorRelation();
		// 加载监听器
		registListener((ProcessDefinitionEntity) processDefinition);
		return processDefinition;
	}
	
	/**
	 * loadBehavior 根据Process模型加载流程定义对象
	 * 
	 * @param process
	 * @return 流程定义对象
	 */
	private KernelProcessDefinition loadProcess(BpmnModel bpmnModel) {
		Process process = bpmnModel.getProcesses().get(0);
		String processObjId = process.getId();
		List<FlowElement> flowElements = process.getFlowElements();
		ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionEntityBuilder(processObjId);
		for (Connector connector : process.getConnector()) {
			ConnectorListener connectorListener = new ConnectorListener();
			connectorListener.setConnector(connector);
			processDefinitionBuilder.executionListener(connector.getEventType(), connectorListener);
		}
		
		for (FlowElement flowElement : flowElements) {
			generateBuilder(processDefinitionBuilder, flowElement, false, process.getSequenceFlows());
		}
		
		ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) processDefinitionBuilder.buildProcessDefinition();
		
		if (process.getLaneSets() != null && process.getLaneSets().size() > 0) {
			for (LaneSet laneSet : process.getLaneSets()) {
				
				KernelLaneSetImpl laneSetObj = new KernelLaneSetImpl(laneSet.getId(), processDefinition);
				laneSetObj.setName(laneSet.getName());
				loadLane(laneSetObj, laneSet, processDefinition);
				
				processDefinition.getLaneSets().add(laneSetObj);
			}
		}
		
		// // 加载其他元素
		// for (Artifact artifact : process.getArtifacts()) {
		// KernelArtifactBehavior artifactBehavior =
		// BpmnBehaviorEMFConverter.getArtifactBehavior(artifact,
		// processDefinitionBuilder.getProcessDefinition());
		// KernelArtifactImpl kernelArtifactImpl = new
		// KernelArtifactImpl(artifact.getId(), processDefinition);
		// if (artifact instanceof Association) {
		// kernelArtifactImpl = new KernelAssociationImpl(artifact.getId(),
		// processDefinition);
		// }
		//
		// kernelArtifactImpl.setArtifactBehavior(artifactBehavior);
		// processDefinition.getArtifacts().add(kernelArtifactImpl);
		//
		// }
		
		processDefinition.setKey(process.getId());
		processDefinition.setName(process.getName());
		processDefinition.setCategory(process.getCategory());
		processDefinition.setFormUri(process.getFormUri());
		processDefinition.setFormUriView(process.getFormUriView());
		processDefinition.setSubject(process.getSubject());
		processDefinition.setPotentialStarters(process.getPotentialStarters());
		processDefinition.setTenantId(process.getTenantId());
		processDefinition.setProperty("documentation", process.getDocumentation());
		
		DataVariableMgmtDefinition dataVariableMgmtDefinition = new DataVariableMgmtDefinition(processDefinition);
		dataVariableMgmtDefinition.getDataVariableDefinitions().addAll(process.getDataVariables());
		processDefinition.setDataVariableMgmtDefinition(dataVariableMgmtDefinition);
		processDI(processDefinition, bpmnModel);
		return processDefinition;
	}
	
	/**
	 * 根据flowElement构造builder(递归内部子流程)
	 * 
	 * @param processDefinitionBuilder
	 * @param flowElement
	 * @param isSub
	 *            是否子流程
	 */
	private void generateBuilder(ProcessDefinitionBuilder processDefinitionBuilder, FlowElement flowElement,
	    boolean isSub, Map<String, SequenceFlow> sequenceFlows) {
		KernelFlowNodeBehavior flowNodeBehavior = BpmnBehaviorEMFConverter.getFlowNodeBehavior(flowElement, processDefinitionBuilder.getProcessDefinition());
		if (flowElement instanceof FlowNode) {
			processDefinitionBuilder.createFlowNode(flowElement.getId(), flowElement.getName()).behavior(flowNodeBehavior);
			KernelFlowNodeImpl kernelFlowNodeImpl = processDefinitionBuilder.getFlowNode();
			// 特殊处理开始节点，如果是子流程，则放到properties属性中，否则，放到processDefinition的initial属性中
			if (flowElement instanceof StartEvent) {
				if (!isSub) {
					processDefinitionBuilder.initial();
				} else {
					kernelFlowNodeImpl.getParent().setProperty("initial", kernelFlowNodeImpl);
				}
			}
			
			// 特殊处理子流程，需要递归处理节点
			if (flowElement instanceof SubProcess) {
				SubProcess subProcess = (SubProcess) flowElement;
				Iterator<FlowElement> flowElements = subProcess.getFlowElements().iterator();
				while (flowElements.hasNext()) {
					FlowElement tmpFlowElement = flowElements.next();
					generateBuilder(processDefinitionBuilder, tmpFlowElement, true, sequenceFlows);
				}
			}
			// 处理连接器
			if (flowNodeBehavior instanceof BaseElementBehavior) {
				for (Connector connector : flowElement.getConnector()) {
					ConnectorListener connectorListener = new ConnectorListener();
					connectorListener.setConnector(connector);
					processDefinitionBuilder.executionListener(connector.getEventType(), connectorListener);
				}
			}
			
			// 处理线条
			List<String> sequenceFlowIds = ((FlowNode) flowElement).getOutgoingFlows();
			for (String sequenceFlowId : sequenceFlowIds) {
				SequenceFlow tmpElement = sequenceFlows.get(sequenceFlowId);
				KernelSequenceFlowBehavior kernelSequenceFlowBehavior = BpmnBehaviorEMFConverter.getSequenceFlowBehavior(tmpElement, processDefinitionBuilder.getProcessDefinition());
				processDefinitionBuilder.sequenceFlow(tmpElement.getTargetRefId(), tmpElement.getId(), tmpElement.getName(), kernelSequenceFlowBehavior);
			}
			processDefinitionBuilder.endFlowNode();
		}
	}
	
	/**
	 * 注册流程定义和流程节点事件监听
	 * @param flowNode
	 * @param eventMapListenerList
	 * @param eventType
	 */
	private void registEventListener(KernelFlowElementsContainerImpl flowNode,Map<String,List<EventListener>> eventMapListenerList,String eventType){
		try{
			List<EventListener> startListeners = eventMapListenerList.get(eventType);
			if(startListeners != null && startListeners.size()>0){ 
				KernelListener foxbpmEventListener = null;
				for(EventListener eventListener : startListeners){
					foxbpmEventListener = (KernelListener) Class.forName(eventListener.getListenerClass()).newInstance();
					flowNode.addKernelListener(eventType, foxbpmEventListener);
				}
				
			}
		} catch (Exception e) {
			throw new FoxBPMException("加载流程定义和流程节点监听器时出现问题", e);
		}
	}
	
	/**
	 * 注册线条事件监听
	 * @param sequenceFlow
	 * @param eventMapListenerList
	 * @param eventType
	 */
	private void registSequenceFlowEventListener(KernelSequenceFlowImpl sequenceFlow,Map<String,List<EventListener>> eventMapListenerList,String eventType){
		try{
			List<EventListener> startListeners = eventMapListenerList.get(eventType);
			if(startListeners != null && startListeners.size()>0){
				KernelListener foxbpmEventListener = null;
				for(EventListener eventListener : startListeners){
					foxbpmEventListener = (KernelListener) Class.forName(eventListener.getListenerClass()).newInstance();
					sequenceFlow.addKernelListener(foxbpmEventListener);
				}
				
			}
		} catch (Exception e) {
			throw new FoxBPMException("加载线条监听器时出现问题", e);
		}
	}
	/**
	 * 加载配置监听器、 独立加载 和嵌入流程定义创建代码中，算法效率是一样的 监听器集合SIZE * 节点集合SIZE
	 * 不建议侵入到流程定义的LOAD代码中
	 * 
	 * @param processEntity
	 */
	private void registListener(ProcessDefinitionEntity processEntity) {  
		Map<String,List<EventListener>> eventMapListenerList = Context.getProcessEngineConfiguration().getEventMapListeners();

		if(eventTypeMap == null || eventMapListenerList == null){ 
			return;
		} 
		//注册流程定义
		List<String> eventList = eventTypeMap.get(PROCESSDEFINITION_EVENT);
		for(String eventType:eventList){
			this.registEventListener(processEntity, eventMapListenerList, eventType);
		}
		 
		 
		// 注册线条监听
		eventList = eventTypeMap.get(SEQUENCEFLOW_EVENT);
		for(String eventType:eventList){
			Map<String, KernelSequenceFlowImpl> sequenceFlows = processEntity.getSequenceFlows();
			Collection<KernelSequenceFlowImpl> sequenceFlowList = sequenceFlows.values(); 
			for(KernelSequenceFlowImpl kernelSequenceFlowImpl : sequenceFlowList){ 
				this.registSequenceFlowEventListener(kernelSequenceFlowImpl, eventMapListenerList, eventType);
			}
			
		}
	 
		//注册流程节点
		eventList = eventTypeMap.get(FLOWNODE_EVENT);
		for(String eventType:eventList){
			// 注册节点监听
			List<KernelFlowNodeImpl> flowNodes = processEntity.getFlowNodes();
			this.registerFlowNodeListener(flowNodes,eventMapListenerList, eventType); 
		} 
	}
	
	/**
	 * 
	 * 子流程节点加载轨迹监听器
	 * 
	 * @param flowNodes
	 * @param eventListener
	 * @param foxbpmEventListener
	 *            void
	 * @exception
	 * @since 1.0.0
	 */ 
	private void registerFlowNodeListener(List<KernelFlowNodeImpl> flowNodes, Map<String,List<EventListener>> eventMapListenerList,String eventType) {
		for (KernelFlowNodeImpl kernelFlowNodeImpl : flowNodes) {
			this.registEventListener(kernelFlowNodeImpl, eventMapListenerList, eventType);
			List<KernelFlowNodeImpl> subFlowNodes = kernelFlowNodeImpl.getFlowNodes();
			if (subFlowNodes != null && subFlowNodes.size() > 0) {
				registerFlowNodeListener(subFlowNodes, eventMapListenerList, eventType);
			}
		}
	}
	
	private void loadLane(KernelLaneSet kernelLaneSet, LaneSet laneSet, ProcessDefinitionEntity processDefinition) {
		kernelLaneSet.setName(laneSet.getName());
		for (Lane lane : laneSet.getLanes()) {
			if (lane != null) {
				KernelLaneImpl KernelLaneImpl = new KernelLaneImpl(lane.getId(), processDefinition);
				KernelLaneImpl.setName(lane.getName());
				kernelLaneSet.getLanes().add(KernelLaneImpl);
				LaneSet childLaneSet = lane.getChildLaneSet();
				if (childLaneSet != null) {
					KernelLaneSetImpl KernelLaneSetImpl = new KernelLaneSetImpl(childLaneSet.getId(), processDefinition);
					KernelLaneSetImpl.setName(childLaneSet.getName());
					KernelLaneImpl.setChildLaneSet(KernelLaneSetImpl);
					loadLane(KernelLaneSetImpl, childLaneSet, processDefinition);
				} else {
					continue;
				}
			}
		}
	}
	
	private void processDI(ProcessDefinitionEntity processDefinition, BpmnModel bpmnModel) {
		
		String processId = bpmnModel.getProcesses().get(0).getId();
		Map<String, Bounds> boundsMap = bpmnModel.getBoundsLocationMap().get(processId);
		Map<String, List<WayPoint>> wayPointsMap = bpmnModel.getWaypointLocationMap().get(processId);
		float maxX = 0;
		float maxY = 0;
		float minY = 0;
		float minX = 0;
		
		for (Bounds tmpBounds : boundsMap.values()) {
			float x = tmpBounds.getX();
			float y = tmpBounds.getY();
			float width = tmpBounds.getWidth();
			float height = tmpBounds.getHeight();
			if (x + width > maxX) {
				maxX = x + width;
			}
			if (y + height > maxY) {
				maxY = y + height;
			}
			if (minY == 0) {
				minY = y;
			} else {
				if (y < minY) {
					minY = y;
				}
			}
			
			if (minX == 0) {
				minX = x;
			} else {
				if (x < minX) {
					minX = x;
				}
			}
			loadBPMNShape(tmpBounds, processDefinition);
		}
		
		for (String edgeId : wayPointsMap.keySet()) {
			List<WayPoint> tmpPointList = wayPointsMap.get(edgeId);
			loadBPMNEdge(tmpPointList, edgeId, processDefinition);
		}
		
		processDefinition.setProperty("canvas_maxX", maxX + 30);
		processDefinition.setProperty("canvas_maxY", maxY + 70);
		processDefinition.setProperty("canvas_minX", minX);
		processDefinition.setProperty("canvas_minY", minY);
	}
	
	/**
	 * 
	 * loadBPMNShape(加载 bpmnShape信息)
	 * 
	 * @param width
	 * @param height
	 * @param x
	 * @param y
	 * @param bpmnShape
	 * @param processDefinition
	 *            void
	 * @exception
	 * @since 1.0.0
	 */
	private void loadBPMNShape(Bounds bounds, ProcessDefinitionEntity processDefinition) {
		
		String elementId = bounds.getBpmnElement();
		
		Style style = null;
		KernelDIBounds kernelDIBounds = this.getDIElementFromProcessDefinition(processDefinition, elementId);
		if (kernelDIBounds != null) {
			if (kernelDIBounds instanceof KernelLane) {
				style = styleContainer.get("Lane");
			} else {
				KernelFlowNodeImpl flowNodeImpl = processDefinition.findFlowNode(elementId);
				if (flowNodeImpl == null) {
					return;
				}
				FlowNodeBehavior behavior = (FlowNodeBehavior) flowNodeImpl.getKernelFlowNodeBehavior();
				style = getStyle(behavior.getBaseElement());
			}
			
			// 图形基本属性
			kernelDIBounds.setWidth((float) bounds.getWidth());
			kernelDIBounds.setHeight((float) bounds.getHeight());
			kernelDIBounds.setX((float) bounds.getX());
			kernelDIBounds.setY((float) bounds.getY());
			// 泳道水平垂直属性
			if (kernelDIBounds instanceof KernelLaneImpl) {
				kernelDIBounds.setProperty(StyleOption.IsHorizontal, bounds.isHorizontal());
			}
			// 内部子流程展开收起属性
			if (kernelDIBounds instanceof KernelFlowNodeImpl
			        && ((KernelFlowNodeImpl) kernelDIBounds).getKernelFlowNodeBehavior() instanceof SubProcessBehavior) {
				kernelDIBounds.setProperty(StyleOption.IsExpanded, bounds.isExpanded());
			}
			// 图形式样属性
			setStyleProperties((KernelBaseElementImpl) kernelDIBounds, style);
		}
	}
	
	/**
	 * 
	 * loadBPMNEdge(加载bpmnEdge信息)
	 * 
	 * @param pointList
	 * @param bpmnEdge
	 * @param processDefinition
	 *            void
	 * @exception
	 * @since 1.0.0
	 */
	private void loadBPMNEdge(List<WayPoint> pointList, String elementId, ProcessDefinitionEntity processDefinition) {
		Style style = null;
		KernelSequenceFlowImpl findSequenceFlow = processDefinition.findSequenceFlow(elementId);
		BaseElement baseElement = ((SequenceFlowBehavior) findSequenceFlow.getSequenceFlowBehavior()).getBaseElement();
		style = getStyle(baseElement);
		List<Integer> waypoints = new ArrayList<Integer>();
		for (WayPoint point : pointList) {
			waypoints.add((int) point.getX());
			waypoints.add((int) point.getY());
		}
		findSequenceFlow.setWaypoints(waypoints);
		if (style != null) {
			setStyleProperties(findSequenceFlow, style);
		}
		// if (bpmnElement instanceof Association) {
		// KernelAssociationImpl kernelAssociationImpl = (KernelAssociationImpl)
		// processDefinition.getKernelArtifactById(bpmnElement.getId());
		// style = processEngineConfiguration.getStyle("Association");
		// List<Integer> waypoints = new ArrayList<Integer>();
		// for (Point point : pointList) {
		// waypoints.add((new Float(point.getX())).intValue());
		// waypoints.add((new Float(point.getY())).intValue());
		// }
		//
		// kernelAssociationImpl.setWaypoints(waypoints);
		// if (style != null) {
		// this.setStyleProperties(kernelAssociationImpl, style);
		// }
		// }
		// if (bpmnElement instanceof MessageFlow) {
		// // TODO MESSAGEFLOW
		// }
	}
	
	/**
	 * 
	 * getDIElementFromProcessDefinition(获取BPMN的DI元素包括所有节点，除了线条之外)
	 * 
	 * @param processDefinition
	 * @param diElementId
	 * @return KernelDIBounds
	 * @exception
	 * @since 1.0.0
	 */
	private KernelDIBounds getDIElementFromProcessDefinition(ProcessDefinitionEntity processDefinition,
	    String diElementId) {
		KernelFlowNodeImpl localFlowNode = processDefinition.getNamedFlowNodes().get(diElementId);
		if (localFlowNode != null) {
			return localFlowNode;
		}
		KernelFlowElementsContainer flowElementsContainer = null;
		KernelFlowNodeImpl nestedFlowNode = null;
		// 返回节点
		for (KernelFlowNodeImpl activity : processDefinition.getFlowNodes()) {
			if (activity instanceof KernelFlowElementsContainer) {
				flowElementsContainer = (KernelFlowElementsContainer) activity;
				nestedFlowNode = (KernelFlowNodeImpl) flowElementsContainer.findFlowNode(diElementId);
				if (nestedFlowNode != null) {
					return nestedFlowNode;
				}
				
			}
			
		}
		
		// 返回泳道
		if (processDefinition.getLaneSets() != null && processDefinition.getLaneSets().size() > 0) {
			KernelLaneImpl lane = null;
			for (KernelLaneSet set : processDefinition.getLaneSets()) {
				lane = (KernelLaneImpl) set.getLaneForId(diElementId);
				if (lane != null) {
					return lane;
				}
			}
		}
		KernelArtifactImpl kernelArtifactImpl = (KernelArtifactImpl) processDefinition.getKernelArtifactById(diElementId);
		// 返回非线条部件
		if (!(kernelArtifactImpl instanceof KernelAssociationImpl)) {
			return kernelArtifactImpl;
		}
		return null;
	}
	
	/**
	 * 
	 * getStyle(获取元素式样)
	 * 
	 * @param bpmnElement
	 * @param processEngineConfiguration
	 * @return Style
	 * @exception
	 * @since 1.0.0
	 */
	private Style getStyle(BaseElement bpmnElement) {
		Style style = styleContainer.get(bpmnElement.getClass().getSimpleName());
		if (style == null) {
			throw new FoxBPMException("未找到" + bpmnElement.getClass() + "的style样式");
		}
		return style;
	}
	
	/**
	 * 
	 * setStyleProperties(设置元素式样)
	 * 
	 * @param kernelBaseElementImpl
	 * @param style
	 * @exception
	 * @since 1.0.0
	 */
	private void setStyleProperties(KernelBaseElementImpl kernelBaseElementImpl, Style style) {
		kernelBaseElementImpl.setProperty(StyleOption.Background, style.getBackGround());
		kernelBaseElementImpl.setProperty(StyleOption.Font, style.getFont());
		kernelBaseElementImpl.setProperty(StyleOption.Foreground, style.getForeGround());
		kernelBaseElementImpl.setProperty(StyleOption.MulitSelectedColor, style.getMulitSelectedColor());
		kernelBaseElementImpl.setProperty(StyleOption.StyleObject, style.getElementType());
		kernelBaseElementImpl.setProperty(StyleOption.SelectedColor, style.getSelectedColor());
		kernelBaseElementImpl.setProperty(StyleOption.TextColor, style.getTextColor());
	}
	
	private BpmnModel loadBpmnModel(String processId, InputStream is) {
		BpmnXMLConverter converter = new BpmnXMLConverter();
		SAXReader reader = new SAXReader();
		BpmnModel bpmnModel = null;
		try {
			Document doc = reader.read(is);
			bpmnModel = converter.convertToBpmnModel(doc);
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return bpmnModel;
	}
	
	/**
	 * 
	 * 
	 * BehaviorRelationMemo
	 * 
	 * MAENLIANG 2014年8月4日 下午1:51:31
	 * 
	 * @version 1.0.0
	 * 
	 */
	public static class BehaviorRelationMemo {
		/** 临时存储MAP */
		private Map<String, ActivityBehavior> attachActivityMap = new HashMap<String, ActivityBehavior>();
		private Map<String, List<BoundaryEventBehavior>> beAttachedActivityMap = new HashMap<String, List<BoundaryEventBehavior>>();
		
		/**
		 * 
		 * attachActivityAndBoundaryEventBehaviorRelation(创建Activity
		 * 和BoundaryEventBehavior之间的关联关系) void
		 * 
		 * @exception
		 * @since 1.0.0
		 */
		public void attachActivityAndBoundaryEventBehaviorRelation() {
			Set<String> keySet = beAttachedActivityMap.keySet();
			for (String activityID : keySet) {
				if (attachActivityMap.containsKey(activityID)) {
					List<BoundaryEventBehavior> list = beAttachedActivityMap.get(activityID);
					for (EventBehavior behavior : list) {
						attachActivityMap.get(activityID).getBoundaryEventBehaviors().add((BoundaryEventBehavior) behavior);
					}
					
				}
			}
			this.attachActivityMap.clear();
			this.beAttachedActivityMap.clear();
		}
		
		/**
		 * 
		 * addActivity(保存解释得到的活动节点)
		 * 
		 * @param activity
		 * @param activityBehavior
		 *            void
		 * @exception
		 * @since 1.0.0
		 */
		public void addActivity(Activity activity, ActivityBehavior activityBehavior) {
			this.attachActivityMap.put(activity.getId(), activityBehavior);
		}
		
		/**
		 * 
		 * addActivity(保存解释得到的事件行为)
		 * 
		 * @param activity
		 * @param eventBehavior
		 *            void
		 * @exception
		 * @since 1.0.0
		 */
		public void addBeAttachedActivity(String activityId, BoundaryEventBehavior eventBehavior) {
			List<BoundaryEventBehavior> list = beAttachedActivityMap.get(activityId);
			if (list == null) {
				list = new ArrayList<BoundaryEventBehavior>();
				this.beAttachedActivityMap.put(activityId, list);
			}
			list.add(eventBehavior);
		}
	}
	
}

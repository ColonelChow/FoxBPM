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
 * @author MAENLIANG
 */
package org.foxbpm.engine.impl.diagramview.svg;

import java.awt.Font;
import java.awt.FontMetrics;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang3.StringUtils;
import org.foxbpm.engine.exception.FoxBPMException;
import org.foxbpm.engine.impl.diagramview.svg.vo.CircleVO;
import org.foxbpm.engine.impl.diagramview.svg.vo.DefsVO;
import org.foxbpm.engine.impl.diagramview.svg.vo.GVO;
import org.foxbpm.engine.impl.diagramview.svg.vo.MarkerVO;
import org.foxbpm.engine.impl.diagramview.svg.vo.PathVO;
import org.foxbpm.engine.impl.diagramview.svg.vo.RectVO;
import org.foxbpm.engine.impl.diagramview.svg.vo.SvgVO;
import org.foxbpm.engine.impl.diagramview.vo.VONode;
import org.foxbpm.engine.impl.util.StringUtil;

/**
 * SVG工具类
 * 
 * @author MAENLIANG
 * @date 2014-06-10
 */
public final class SVGUtils {
	
	/**
	 * BPMN节点类型(例如：矩形，圆形)在SVG文档中的ID
	 */
	private static final String BPMN_NODE_ID = "bg_frame";
	private static final String EDGE = "edge";
	private static final String SPACE = " ";
	/**
	 * 获取任务矩形
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static RectVO getTaskVOFromSvgVO(SvgVO svgVo) {
		List<RectVO> rectVoList = svgVo.getgVo().getRectVoList();
		Iterator<RectVO> iterator = rectVoList.iterator();
		RectVO next = null;
		while (iterator.hasNext()) {
			next = iterator.next();
			if (StringUtils.equalsIgnoreCase(next.getId(), BPMN_NODE_ID)) {
				return next;
			}
		}
		return null;
	}
	
	/**
	 * 获取任务矩形
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static PathVO getSequentialVOFromSvgVO(SvgVO svgVo) {
		List<GVO> gVoList = svgVo.getgVo().getgVoList();
		if(gVoList != null){
			for (GVO gvo : gVoList) {
				if (StringUtil.equals(gvo.getId(), "sequential")) {
					return gvo.getPathVoList().get(0);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 获取事件圆形
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static CircleVO getEventVOFromSvgVO(SvgVO svgVo) {
		List<CircleVO> circleVoList = svgVo.getgVo().getCircleVoList();
		Iterator<CircleVO> iterator = circleVoList.iterator();
		CircleVO next = null;
		while (iterator.hasNext()) {
			next = iterator.next();
			if (StringUtils.equalsIgnoreCase(next.getId(), BPMN_NODE_ID)) {
				return next;
			}
		}
		return null;
	}
	
	/**
	 * 获取事件圆形
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static CircleVO getEndTerminateEventVOFromSvgVO(SvgVO svgVo) {
		List<CircleVO> circleVoList = svgVo.getgVo().getCircleVoList();
		Iterator<CircleVO> iterator = circleVoList.iterator();
		CircleVO next = null;
		while (iterator.hasNext()) {
			next = iterator.next();
			if (StringUtils.equalsIgnoreCase(next.getId(), "circle1")) {
				return next;
			}
		}
		return null;
	}
	
	/**
	 * 获取事件圆形
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static CircleVO getDefinitionEventVOFromSvgVO(SvgVO svgVo, String id) {
		List<GVO> getgVoList = svgVo.getgVo().getgVoList();
		Iterator<GVO> gvoIter = getgVoList.iterator();
		while (gvoIter.hasNext()) {
			GVO gvo = gvoIter.next();
			List<CircleVO> circleVoList = gvo.getCircleVoList();
			Iterator<CircleVO> iterator = circleVoList.iterator();
			CircleVO next = null;
			while (iterator.hasNext()) {
				next = iterator.next();
				if (StringUtils.equalsIgnoreCase(next.getId(), id)) {
					return next;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 获取线条路径
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static PathVO getSequenceVOFromSvgVODirectly(SvgVO svgVo) {
		GVO getgVo = svgVo.getgVo();
		if (getgVo != null) {
			List<PathVO> pathVoList = getgVo.getPathVoList();
			Iterator<PathVO> iterator = pathVoList.iterator();
			PathVO tempPathVo = null;
			while (iterator.hasNext()) {
				tempPathVo = iterator.next();
				if (StringUtils.equalsIgnoreCase(tempPathVo.getId(), BPMN_NODE_ID)) {
					return tempPathVo;
				}
			}
		}
		return null;
	}
	
	/**
	 * 获取线条路径
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static PathVO getSequenceMarkerVOFromSvgVO(SvgVO svgVo) {
		List<MarkerVO> markerVOList = svgVo.getgVo().getgVoList().get(0).getDefsVo().getMarkerVOList();
		for (MarkerVO markerVo : markerVOList) {
			if (StringUtil.equals(markerVo.getId(), "end")) {
				List<PathVO> pathVOList = markerVo.getPathVOList();
				return pathVOList.get(0);
			}
			
		}
		return null;
	}
	
	/**
	 * 获取线条路径
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static PathVO getSequenceVOFromSvgVO(SvgVO svgVo) {
		List<GVO> gVoList = svgVo.getgVo().getgVoList();
		if (gVoList != null && gVoList.size() > 0) {
			Iterator<GVO> iterator = gVoList.iterator();
			GVO next = null;
			List<PathVO> pathVoList = null;
			Iterator<PathVO> pathIter = null;
			PathVO tempPathVo = null;
			while (iterator.hasNext()) {
				next = iterator.next();
				if (StringUtils.equalsIgnoreCase(next.getId(), EDGE)) {
					pathVoList = next.getPathVoList();
					pathIter = pathVoList.iterator();
					while (pathIter.hasNext()) {
						tempPathVo = pathIter.next();
						if (StringUtils.equalsIgnoreCase(tempPathVo.getId(), BPMN_NODE_ID)) {
							return tempPathVo;
						}
					}
					
				}
			}
		}
		return null;
	}
	
	/**
	 * 获取网关路径
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static PathVO getGatewayVOFromSvgVO(SvgVO svgVo) {
		List<PathVO> pathVoList = svgVo.getgVo().getPathVoList();
		Iterator<PathVO> pathIter = pathVoList.iterator();
		PathVO tempPathVo = null;
		while (pathIter.hasNext()) {
			tempPathVo = pathIter.next();
			if (StringUtils.equalsIgnoreCase(tempPathVo.getId(), BPMN_NODE_ID)) {
				return tempPathVo;
			}
		}
		
		return null;
	}
	
	/**
	 * 中文两字节，英文是一字节所以要区分中英文
	 * 
	 * @param c
	 * @return 判断是否是两字节
	 */
	public final static boolean isChinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
		        || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
		        || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
		        || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
		        || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
		        || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
			
			return true;
		}
		return false;
	}
	
	/**
	 * 根据文本的式样，以及文本内容，获取文本在屏幕上展示的像素宽
	 * 
	 * @param font
	 *            字体式样
	 * @param text
	 *            文本
	 * @return 文本宽度
	 */
	public final static int getTextWidth(Font font, String text) {
		JLabel label = new JLabel(text);
		label.setFont(font);
//		Font font = new Font("", style, 12)
		FontMetrics metrics = label.getFontMetrics(new Font("Times New Roman",Font.ITALIC,18));
		return metrics.stringWidth(label.getText());
	}
	
	/**
	 * 将所有的线条的所有点信息，转化成点的坐标
	 * 
	 * @param waypoints
	 * @return
	 */
	public final static List<Point> convertWaypointsTOPointList(List<Integer> waypoints) {
		int size;
		if (waypoints != null && (size = waypoints.size()) > 0 && size % 2 == 0) {
			List<Point> pointList = new ArrayList<Point>();
			Point point = null;
			
			for (int i = 0; i < size; i++) {
				if (i % 2 != 0) {
					point = new Point(waypoints.get(i - 1), waypoints.get(i));
					pointList.add(point);
				}
			}
			return pointList;
		} else {
			throw new FoxBPMException("线条节点有问题 waypoints不符合规则！");
		}
	}
	
	/**
	 * 创建waypoint节点数组
	 * 
	 * @param waypoints
	 * @return
	 */
	public final static String[] getSequenceFLowWayPointArrayByWayPointList(List<Integer> waypoints) {
		int wayPointSize;
		if (waypoints != null && (wayPointSize = waypoints.size()) > 0 && wayPointSize % 2 == 0) {
			String[] wayPointArray = new String[wayPointSize / 2];
			int arrayIndex = 0;
			
			for (int i = 0; i < wayPointSize; i++) {
				if (i % 2 != 0) {
					wayPointArray[arrayIndex] = String.valueOf(waypoints.get(i - 1)) + SPACE
					        + String.valueOf(waypoints.get(i)) + SPACE;
					arrayIndex++;
				}
			}
			return wayPointArray;
		} else {
			throw new FoxBPMException("线条节点有问题 waypoints不符合规则！");
		}
	}
	
	/**
	 * 操作之后的SVG转化成String字符串
	 * 
	 * @param svgVo
	 * @return
	 */
	public final static String createSVGString(VONode svgVo) {
		try {
			JAXBContext context = JAXBContext.newInstance(SvgVO.class);
			Marshaller marshal = context.createMarshaller();
			marshal.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter writer = new StringWriter();
			
			marshal.marshal(svgVo, writer);
			return writer.toString();
		} catch (Exception e) {
			throw new FoxBPMException("svg object convert to String exception", e);
		}
	}
	
	/**
	 * DefsVO克隆
	 * 
	 * @param DefsVO
	 *            原对象
	 * @return clone之后的对象
	 */
	public final static GVO cloneGVO(GVO gVo) {
		return (GVO) clone(gVo);
	}
	public final static DefsVO cloneDefVO(DefsVO defsVO) {
		return (DefsVO) clone(defsVO);
	}
	
	public final static DefsVO cloneDefsVO(DefsVO defsVo) {
		return (DefsVO) clone(defsVo);
	}
	
	/**
	 * SvgVO模板对象需要多次引用，所以要克隆，避免产生问题
	 * 
	 * @param SvgVO
	 *            原对象
	 * @return clone之后的对象
	 */
	public final static SvgVO cloneSVGVo(SvgVO svgVo) {
		return (SvgVO) clone(svgVo);
	}
	
	/**
	 * 克隆对象
	 * 
	 * @param object
	 *            原对象
	 * @return 目标对象
	 */
	public final static Object clone(Object object) {
		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		Object cloneObject = null;
		try {
			bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(object);
			ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
			cloneObject = ois.readObject();
		} catch (Exception e) {
			throw new FoxBPMException("SVG对象G节点克隆出现问题", e);
		} finally {
			try {
				if (bos != null) {
					bos.close();
				}
				if (oos != null) {
					oos.close();
				}
				if (ois != null) {
					ois.close();
				}
			} catch (Exception e) {
				throw new FoxBPMException("克隆之后关闭对象流时出现问题", e);
			}
		}
		
		return cloneObject;
	}
}

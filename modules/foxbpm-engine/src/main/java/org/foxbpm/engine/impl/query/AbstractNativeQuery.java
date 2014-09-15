
package org.foxbpm.engine.impl.query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.foxbpm.engine.exception.FoxBPMException;
import org.foxbpm.engine.impl.Context;
import org.foxbpm.engine.impl.interceptor.Command;
import org.foxbpm.engine.impl.interceptor.CommandContext;
import org.foxbpm.engine.impl.interceptor.CommandExecutor;
import org.foxbpm.engine.impl.util.StringUtil;
import org.foxbpm.engine.query.NativeQuery;


public abstract class AbstractNativeQuery<T extends NativeQuery< ? , ? >, U> implements Command<Object>, NativeQuery<T, U>,
        Serializable {

  private static final long serialVersionUID = 1L;

  private static enum ResultType {
    LIST, LIST_PAGE, SINGLE_RESULT, COUNT
  }

  protected transient CommandExecutor commandExecutor;
  protected transient CommandContext commandContext;

  protected int maxResults = Integer.MAX_VALUE;
  protected int firstResult = 0;
  protected ResultType resultType;

  private Map<String, Object> parameters = new HashMap<String, Object>();
  private String sqlStatement;

  protected AbstractNativeQuery(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  public AbstractNativeQuery(CommandContext commandContext) {
    this.commandContext = commandContext;
  }

  public AbstractNativeQuery<T, U> setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return this;
  }

  @SuppressWarnings("unchecked")
  public T sql(String sqlStatement) {
    this.sqlStatement = sqlStatement;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T parameter(String name, Object value) {
    parameters.put(name, value);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public U singleResult() {
    this.resultType = ResultType.SINGLE_RESULT;
    if (commandExecutor != null) {
      return (U) commandExecutor.execute(this);
    }
    return executeSingleResult(Context.getCommandContext());
  }

  @SuppressWarnings("unchecked")
  public List<U> list() {
    this.resultType = ResultType.LIST;
    if (commandExecutor != null) {
      return (List<U>) commandExecutor.execute(this);
    }
    return executeList(Context.getCommandContext(), getParameterMap(), 0, Integer.MAX_VALUE);
  }
  
  @SuppressWarnings("unchecked")
  public List<U> listPage(int firstResult, int maxResults) {
    this.firstResult =firstResult;
    this.maxResults = maxResults;
    this.resultType = ResultType.LIST_PAGE;
    if (commandExecutor!=null) {
      return (List<U>) commandExecutor.execute(this);
    }
    return executeList(Context.getCommandContext(), getParameterMap(), firstResult, maxResults);
  }

  public long count() {
    this.resultType = ResultType.COUNT;
    if (commandExecutor != null) {
      return (Long) commandExecutor.execute(this);
    }
    return executeCount(Context.getCommandContext(), getParameterMap());
  }

  public Object execute(CommandContext commandContext) {
    if (resultType == ResultType.LIST) {
      return executeList(commandContext, getParameterMap(), 0, Integer.MAX_VALUE);
    } else if (resultType == ResultType.LIST_PAGE) {
      Map<String, Object> parameterMap = getParameterMap();
      parameterMap.put("resultType", "LIST_PAGE");
      parameterMap.put("firstResult", firstResult);
      parameterMap.put("maxResults", maxResults);
      if (StringUtil.isNotEmpty(ObjectUtils.toString(parameterMap.get("orderBy")))) {
        parameterMap.put("orderBy", "RES." + parameterMap.get("orderBy"));
      } else {
        parameterMap.put("orderBy", "RES.ID_ asc");
      }
      
      int firstRow = firstResult + 1;
      parameterMap.put("firstRow", firstRow);
      int lastRow = 0;
      if(maxResults == Integer.MAX_VALUE) {
        lastRow = maxResults;
      }
      lastRow = firstResult + maxResults + 1;
      parameterMap.put("lastRow", lastRow);
      return executeList(commandContext, parameterMap, firstResult, maxResults);
    } else if (resultType == ResultType.SINGLE_RESULT) {
      return executeSingleResult(commandContext);
    } else {
      return executeCount(commandContext, getParameterMap());
    }
  }

  public abstract long executeCount(CommandContext commandContext, Map<String, Object> parameterMap);

  /**
   * Executes the actual query to retrieve the list of results.
   * @param maxResults 
   * @param firstResult 
   * 
   * @param page
   *          used if the results must be paged. If null, no paging will be
   *          applied.
   */
  public abstract List<U> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults);

  public U executeSingleResult(CommandContext commandContext) {
    List<U> results = executeList(commandContext, getParameterMap(), 0, Integer.MAX_VALUE);
    if (results.size() == 1) {
      return results.get(0);
    } else if (results.size() > 1) {
      throw new FoxBPMException("Query return " + results.size() + " results instead of max 1");
    }
    return null;
  }

  private Map<String, Object> getParameterMap() {
    HashMap<String, Object> parameterMap = new HashMap<String, Object>();
    parameterMap.put("sql", sqlStatement);
    parameterMap.putAll(parameters);
    return parameterMap;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

}

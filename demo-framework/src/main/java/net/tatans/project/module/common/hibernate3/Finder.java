package net.tatans.project.module.common.hibernate3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.Type;

/**
 * Project Name:demo-framework
 * 
 * Package:net.tatans.demo.common.hibernate3
 * 
 * FileName:Finder.java
 * 
 * @author Jie Kim
 * 
 *         Purpose:HQL语句分页查询 。 Finder封装了hql语句和该hql语句所对应的总记录数的语句(select
 *         count(*)), 以及该hql语句对应的参数。它弥补了HQL在动态查询时过于繁琐的缺点。
 * 
 *         Create Time: 2014年5月20日 下午5:37:07
 * 
 *         Create Specification:
 * 
 *         Modified Time:
 * 
 *         Modified by:
 * 
 *         Modified Specification:
 * 
 *         Version: 1.0
 */
public class Finder
{
	protected Finder()
	{
		hqlBuilder = new StringBuilder();
	}

	protected Finder(String hql)
	{
		hqlBuilder = new StringBuilder(hql);
	}

	public static Finder create()
	{
		return new Finder();
	}

	public static Finder create(String hql)
	{
		return new Finder(hql);
	}

	public Finder append(String hql)
	{
		hqlBuilder.append(hql);
		return this;
	}

	/**
	 * 获得原始hql语句
	 * 
	 * @return
	 */
	public String getOrigHql()
	{
		return hqlBuilder.toString();
	}

	/**
	 * 获得查询数据库记录数的hql语句。
	 * 
	 * @return
	 */
	public String getRowCountHql()
	{
		// 对于from之前的hql语句要转换为select count(*)或select count(属性)
//		 from之后的hql语句要去掉order by和fetch关键字//
		String hql = hqlBuilder.toString();

		int fromIndex = hql.toLowerCase().indexOf(FROM);
		String projectionHql = hql.substring(0, fromIndex);// from之前的hql语句（投影列Hql）

		hql = hql.substring(fromIndex);// from之后的hql语句
		String rowCountHql = hql.replace(HQL_FETCH, "");// 获得查询数据库记录数的hql语句,先把fetch给去掉

		int index = rowCountHql.indexOf(ORDER_BY);
		if (index > 0)
		{
			// 如果有order,去掉包括order的order之后的语句
			rowCountHql = rowCountHql.substring(0, index);
		}
		return wrapProjection(projectionHql) + rowCountHql;
	}

	public int getFirstResult()
	{
		return firstResult;
	}

	public void setFirstResult(int firstResult)
	{
		this.firstResult = firstResult;
	}

	public int getMaxResults()
	{
		return maxResults;
	}

	public void setMaxResults(int maxResults)
	{
		this.maxResults = maxResults;
	}

	/**
	 * 是否使用查询缓存
	 * 
	 * @return
	 */
	public boolean isCacheable()
	{
		return cacheable;
	}

	/**
	 * 设置是否使用查询缓存
	 * 
	 * @param cacheable
	 * @see Query#setCacheable(boolean)
	 */
	public void setCacheable(boolean cacheable)
	{
		this.cacheable = cacheable;
	}

	/**
	 * 设置参数
	 * 
	 * @param param
	 * @param value
	 * @return
	 * @see Query#setParameter(String, Object)
	 */
	public Finder setParam(String param, Object value)
	{
		return setParam(param, value, null);
	}

	/**
	 * 设置参数。与hibernate的Query接口一致。
	 * 
	 * @param param
	 * @param value
	 * @param type
	 * @return
	 * @see Query#setParameter(String, Object, Type)
	 */
	public Finder setParam(String param, Object value, Type type)
	{
		getParams().add(param);
		getValues().add(value);
		getTypes().add(type);
		return this;// 链式编程
	}

	/**
	 * 设置参数。与hibernate的Query接口一致。
	 * 
	 * @param paramMap
	 * @return
	 * @see Query#setProperties(Map)
	 */
	public Finder setParams(Map<String, Object> paramMap)
	{
		for (Map.Entry<String, Object> entry : paramMap.entrySet())
		{
			setParam(entry.getKey(), entry.getValue());
		}
		return this;
	}

	/**
	 * 设置参数。与hibernate的Query接口一致。
	 * 
	 * @param name
	 * @param vals
	 * @param type
	 * @return
	 * @see Query#setParameterList(String, Collection, Type))
	 */
	public Finder setParamList(String name, Collection<Object> vals, Type type)
	{
		getParamsList().add(name);
		getValuesList().add(vals);
		getTypesList().add(type);
		return this;
	}

	/**
	 * 设置参数。与hibernate的Query接口一致。
	 * 
	 * @param name
	 * @param vals
	 * @return
	 * @see Query#setParameterList(String, Collection)
	 */
	public Finder setParamList(String name, Collection<Object> vals)
	{
		return setParamList(name, vals, null);
	}

	/**
	 * 设置参数。与hibernate的Query接口一致。
	 * 
	 * @param name
	 * @param vals
	 * @param type
	 * @return
	 * @see Query#setParameterList(String, Object[], Type)
	 */
	public Finder setParamList(String name, Object[] vals, Type type)
	{
		getParamsArray().add(name);
		getValuesArray().add(vals);
		getTypesArray().add(type);
		return this;
	}

	/**
	 * 设置参数。与hibernate的Query接口一致。
	 * 
	 * @param name
	 * @param vals
	 * @return
	 * @see Query#setParameterList(String, Object[])
	 */
	public Finder setParamList(String name, Object[] vals)
	{
		return setParamList(name, vals, null);
	}

	/**
	 * 将finder中的参数设置到query中。
	 * 
	 * @param query
	 */
	public Query setParamsToQuery(Query query)
	{
		if (params != null)
		{
			for (int i = 0; i < params.size(); i++)
			{
				if (types.get(i) == null)
				{
					query.setParameter(params.get(i), values.get(i));
				} else
				{
					query.setParameter(params.get(i), values.get(i),
							types.get(i));
				}
			}
		}
		if (paramsList != null)
		{
			for (int i = 0; i < paramsList.size(); i++)
			{
				if (typesList.get(i) == null)
				{
					query.setParameterList(paramsList.get(i), valuesList.get(i));
				} else
				{
					query.setParameterList(paramsList.get(i),
							valuesList.get(i), typesList.get(i));
				}
			}
		}
		if (paramsArray != null)
		{
			for (int i = 0; i < paramsArray.size(); i++)
			{
				if (typesArray.get(i) == null)
				{
					query.setParameterList(paramsArray.get(i),
							valuesArray.get(i));
				} else
				{
					query.setParameterList(paramsArray.get(i),
							valuesArray.get(i), typesArray.get(i));
				}
			}
		}
		return query;
	}

	/**
	 * Purpose: 根据hql创建query对象，并赋予参数
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午5:44:47
	 * 
	 * @param s
	 * @return
	 * 
	 *         Version: 1.0
	 */
	public Query createQuery(Session s)
	{
		Query query = setParamsToQuery(s.createQuery(getOrigHql()));
		if (getFirstResult() > 0)
		{
			query.setFirstResult(getFirstResult());
		}
		if (getMaxResults() > 0)
		{
			query.setMaxResults(getMaxResults());
		}
		if (isCacheable())
		{
			query.setCacheable(true);
		}
		return query;
	}

	/**
	 * Purpose:包装hql语句的投影列，使之符合count()的形势
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午5:45:11
	 * 
	 * @param projection
	 *            from之前的hql语句(投影列)
	 * @return
	 * 
	 *         Version: 1.0
	 */
	private String wrapProjection(String projection)
	{
		if (projection.indexOf("select") == -1)
		{
			// 如果没有select关键字，直接返回select count(*)
			return ROW_COUNT;
		} else
		{
			// 将select关键字替换为"select count("，然后再加上")"
			return projection.replace("select", "select count(") + ") ";
		}
	}

	private List<String> getParams()
	{
		if (params == null)
		{
			params = new ArrayList<String>();
		}
		return params;
	}

	private List<Object> getValues()
	{
		if (values == null)
		{
			values = new ArrayList<Object>();
		}
		return values;
	}

	private List<Type> getTypes()
	{
		if (types == null)
		{
			types = new ArrayList<Type>();
		}
		return types;
	}

	private List<String> getParamsList()
	{
		if (paramsList == null)
		{
			paramsList = new ArrayList<String>();
		}
		return paramsList;
	}

	private List<Collection<Object>> getValuesList()
	{
		if (valuesList == null)
		{
			valuesList = new ArrayList<Collection<Object>>();
		}
		return valuesList;
	}

	private List<Type> getTypesList()
	{
		if (typesList == null)
		{
			typesList = new ArrayList<Type>();
		}
		return typesList;
	}

	private List<String> getParamsArray()
	{
		if (paramsArray == null)
		{
			paramsArray = new ArrayList<String>();
		}
		return paramsArray;
	}

	private List<Object[]> getValuesArray()
	{
		if (valuesArray == null)
		{
			valuesArray = new ArrayList<Object[]>();
		}
		return valuesArray;
	}

	private List<Type> getTypesArray()
	{
		if (typesArray == null)
		{
			typesArray = new ArrayList<Type>();
		}
		return typesArray;
	}

	private StringBuilder hqlBuilder;// StringBuilder类型的hql语句StringBuilder。方便拼接，减小内存消耗。

	/**
	 * Hibernater Query接口中 public Query setParameter(String name,Object val,Typetype)参数(普通类型) 
	 * public Query setParameterList(String name,Collectionvals,Type type)参数(集合类型) 
	 * public Query setParameterList(String name,Object[] vals,Type type)参数(数组类型)
	 * 因为Finder的参数绑定底层还是调用了Query的以上方法，所以设计成一样的接口方便底层实现和接口API使用
	 */
	private List<String> params;// 参数名称
	private List<Object> values;// 参数(普通类型)
	private List<Type> types;// 参数类型

	private List<String> paramsList;// 参数名称
	private List<Collection<Object>> valuesList;// 参数(集合类型)
	private List<Type> typesList;// 参数类型

	private List<String> paramsArray;// 参数名称
	private List<Object[]> valuesArray;// 参数(数组类型)
	private List<Type> typesArray;// 参数类型
	/**
	 * 用于分页
	 */
	private int firstResult = 0;
	private int maxResults = 0;

	private boolean cacheable = false;

	public static final String ROW_COUNT = "select count(*) ";
	public static final String FROM = "from";
	public static final String DISTINCT = "distinct";
	public static final String HQL_FETCH = "fetch";
	public static final String ORDER_BY = "order";

	
	/**
	 * @TODO 建议使用junit4，以后补充
	 * Purpose:测试
	 * 
	 * @author Jie Kim
	 * 
	 * Create Time: 2014年5月20日 下午5:46:24
	 * 
	 * @param args
	 * 
	 * Version: 1.0
	 */
	public static void main(String[] args)
	{
		Finder find = Finder
				.create("select distinct p FROM BookType join fetch p");
		System.out.println(find.getRowCountHql());
		System.out.println(find.getOrigHql());
		System.out.println("----------------------------");
		Finder find1 = Finder
				.create("select distinct b.p FROM BookType b join fetch b.p order by b.de");
		System.out.println(find1.getRowCountHql());
		System.out.println(find1.getOrigHql());
		System.out.println("----------------------------");
		Finder find2 = Finder
				.create("FROM BookType b join fetch b.p order by b.de");
		System.out.println(find2.getRowCountHql());
		System.out.println(find2.getOrigHql());
	}

}
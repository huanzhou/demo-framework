package net.tatans.project.module.common.hibernate3;

import java.util.ArrayList;
import java.util.List;

import net.tatans.project.module.common.page.Pagination;
import net.tatans.project.module.common.util.MyBeanUtils;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.transform.ResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Project Name:demo-framework
 * 
 * Package:net.tatans.demo.common.hibernate3
 * 
 * FileName:HibernateSimpleDao.java
 * 
 * @author Jie Kim
 * 
 *         Purpose: hibernate DAO基类<br>
 *         提供hql分页查询，不带泛型，与具体实体类无关。
 * 
 *         Create Time: 2014年5月20日 下午3:25:57
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
public abstract class HibernateSimpleDao
{
	/**
	 * 日志，可用于子类
	 */
	protected Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * hibernate 的 order 属性
	 */
	protected static final String ORDER_ENTRIES = "orderEntries";

	/**
	 * Purpose: 通过HQL查询对象列表
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午3:24:25
	 * 
	 * @param hql
	 *            hql语句
	 * @param values
	 *            数量可变的参数
	 * @return
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings({ "rawtypes" })
	protected List find(String hql, Object... values)
	{
		return createQuery(hql, values).list();
	}

	/**
	 * Purpose: 通过HQL查询唯一对象
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午3:24:58
	 * 
	 * @param hql
	 * @param values
	 * @return
	 * 
	 *         Version: 1.0
	 */
	protected Object findUnique(String hql, Object... values)
	{
		return createQuery(hql, values).uniqueResult();
	}

	/**
	 * Purpose: 通过Finder获得分页数据
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午3:25:20
	 * 
	 * @param finder
	 *            Finder对象。finder里可求出Pagination需要的totalCount和list结果集合,
	 *            firstResult和maxResults以及hql语句和它的参数
	 * @param pageNo
	 *            页码
	 * @param pageSize
	 *            每页条数
	 * @return Pagination 分页对象
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings("rawtypes")
	protected Pagination find(Finder finder, int pageNo, int pageSize)
	{
		int totalCount = countQueryResult(finder);
		Pagination p = new Pagination(pageNo, pageSize, totalCount);
		if (totalCount < 1)
		{
			p.setList(new ArrayList());
			return p;
		}
		Query query = getSession().createQuery(finder.getOrigHql());
		finder.setParamsToQuery(query);
		query.setFirstResult(p.getFirstResult());
		query.setMaxResults(p.getPageSize());
		if (finder.isCacheable())
		{
			query.setCacheable(true);
		}
		List list = query.list();
		p.setList(list);
		return p;
	}

	/**
	 * Purpose: 通过Finder获得列表数据
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午3:23:55
	 * 
	 * @param finder
	 * @return
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings("rawtypes")
	protected List find(Finder finder)
	{
		Query query = finder.createQuery(getSession());
		List list = query.list();
		return list;
	}

	/**
	 * Purpose: 通过Criteria获得分页数据
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午3:14:11
	 * 
	 * @param crit
	 *            条件查询对象Criteria
	 * @param pageNo
	 *            当前页数
	 * @param pageSize
	 *            每页记录数
	 * @return Pagination 分页对象
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Pagination findByCriteria(Criteria crit, int pageNo, int pageSize)
	{
		CriteriaImpl impl = (CriteriaImpl) crit;
		// 先把Projection、ResultTransformer、OrderBy取出来,清空三者后再执行Count操作
		Projection projection = impl.getProjection();
		ResultTransformer transformer = impl.getResultTransformer();
		List<CriteriaImpl.OrderEntry> orderEntries;
		try
		{
			orderEntries = (List) MyBeanUtils
					.getFieldValue(impl, ORDER_ENTRIES);
			MyBeanUtils.setFieldValue(impl, ORDER_ENTRIES, new ArrayList());
		} catch (Exception e)
		{
			throw new RuntimeException(
					"cannot read/write 'orderEntries' from CriteriaImpl", e);
		}

		int totalCount = ((Number) crit.setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		Pagination p = new Pagination(pageNo, pageSize, totalCount);
		if (totalCount < 1)
		{
			p.setList(new ArrayList());
			return p;
		}

		// 将之前的Projection,ResultTransformer和OrderBy条件重新设回去
		crit.setProjection(projection);// 此处projection会覆盖上面的Projections.rowCount()
		if (projection == null)
		{

			/**
			 * 设置下criteria的查询结果，ROOT_ENTITY表示仅仅返回根节点的实例
			 * 如果不设置setProjection()的话，crit.list将返回的是行数(int型)，
			 * 当projection不为null则返回的应该是投影组成的数组对象的集合，
			 * 当projection为null则我们希望返回的应该是实体类型集合
			 * 但是setProjection()会改变Criteria的结果类型，
			 * Criteria的ResultTransformer会变成PassThroughResultTransformer
			 * (不再是以ROOT_ENTITY实体类型返回而是以PROJECTIONObject[]的形式返回)，
			 * criteria.list的时候可能结果会跟理想的不一样。所以我们还要再
			 * crit.setResultTransformer(Criteria
			 * .ROOT_ENTITY);把结果以Entity的形式返回，而不是Object[]的形式返回。
			 */
			crit.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
		}
		if (transformer != null)
		{
			crit.setResultTransformer(transformer);
		}
		try
		{
			MyBeanUtils.setFieldValue(impl, ORDER_ENTRIES, orderEntries);
		} catch (Exception e)
		{
			throw new RuntimeException(
					"set 'orderEntries' to CriteriaImpl faild", e);
		}
		crit.setFirstResult(p.getFirstResult());
		crit.setMaxResults(p.getPageSize());
		p.setList(crit.list());
		return p;
	}

	/**
	 * Purpose:根据查询函数与参数列表创建Query对象,后续可进行更多处理,辅助函数.
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午2:00:24
	 * 
	 * @param queryString
	 *            查询函数
	 * @param values
	 *            查询参数列表
	 * @return
	 * 
	 *         Version: 1.0
	 */
	protected Query createQuery(String queryString, Object... values)
	{
		Assert.hasText(queryString);
		Query queryObject = getSession().createQuery(queryString);
		if (values != null)
		{
			for (int i = 0; i < values.length; i++)
			{
				queryObject.setParameter(i, values[i]);
			}
		}
		return queryObject;
	}

	/**
	 * Purpose: 获得Finder的记录总数
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午3:22:40
	 * 
	 * @param finder
	 * @return
	 * 
	 *         Version: 1.0
	 */
	protected int countQueryResult(Finder finder)
	{
		Query query = getSession().createQuery(finder.getRowCountHql());
		finder.setParamsToQuery(query);
		if (finder.isCacheable())
		{
			query.setCacheable(true);
		}
		return ((Number) query.iterate().next()).intValue();
	}

	protected SessionFactory sessionFactory;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory)
	{
		this.sessionFactory = sessionFactory;
	}

	protected Session getSession()
	{
		return sessionFactory.getCurrentSession();
	}
}

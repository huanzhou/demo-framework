package net.tatans.project.module.common.hibernate3;

import static org.hibernate.EntityMode.POJO;

import java.io.Serializable;
import java.util.List;

import net.tatans.project.module.common.util.MyBeanUtils;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.util.Assert;

/**
 * Project Name:demo-framework
 * 
 * Package:net.tatans.demo.common.hibernate3
 * 
 * FileName:HibernateBaseDao.java
 * 
 * @author Jie Kim
 * 
 *         Purpose: hibernate DAO基类<br>
 *         提供QBC分页查询，拷贝更新等一些常用功能
 * 
 *         Create Time: 2014年5月20日 下午4:17:58
 * 
 *         Create Specification:
 * 
 *         Modified Time:
 * 
 *         Modified by:
 * 
 *         Modified Specification:
 * 
 * @param <T>
 *            entity class
 * @param <ID>
 *            entity id
 * 
 *            Version: 1.0
 */
public abstract class HibernateBaseDao<T, ID extends Serializable> extends
		HibernateSimpleDao
{

	/**
	 * Purpose:
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:28:02
	 * @see Session.get(Class,Serializable)
	 * @param id
	 * @return 持久化对象
	 * 
	 *         Version: 1.0
	 */
	protected T get(ID id)
	{
		return get(id, false);
	}

	/**
	 * Purpose:
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:28:38
	 * @see Session.get(Class,Serializable,LockMode)
	 * @param id
	 *            对象ID
	 * @param lock
	 *            是否锁定，使用LockMode.UPGRADE
	 * @return 持久化对象
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings("unchecked")
	protected T get(ID id, boolean lock)
	{
		T entity;
		if (lock)
		{
			entity = (T) getSession().get(getEntityClass(), id,
					LockMode.UPGRADE);
		} else
		{
			entity = (T) getSession().get(getEntityClass(), id);
		}
		return entity;
	}

	/**
	 * Purpose: 按属性查找对象列表
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:30:39
	 * 
	 * @param property
	 * @param value
	 * @return
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings("unchecked")
	protected List<T> findByProperty(String property, Object value)
	{
		Assert.hasText(property);
		return createCriteria(Restrictions.eq(property, value)).list();
	}

	/**
	 * Purpose: 按属性查找唯一对象
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:31:14
	 * 
	 * @param property
	 * @param value
	 * @return
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings("unchecked")
	protected T findUniqueByProperty(String property, Object value)
	{
		Assert.hasText(property);
		Assert.notNull(value);
		return (T) createCriteria(Restrictions.eq(property, value))
				.uniqueResult();
	}

	/**
	 * Purpose: 按属性统计记录数
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:32:31
	 * 
	 * @param property
	 * @param value
	 * @return
	 * 
	 *         Version: 1.0
	 */
	protected int countByProperty(String property, Object value)
	{
		Assert.hasText(property);
		Assert.notNull(value);
		return ((Number) (createCriteria(Restrictions.eq(property, value))
				.setProjection(Projections.rowCount()).uniqueResult()))
				.intValue();
	}

	/**
	 * Purpose: 按Criterion查询列表数据.
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:32:45
	 * 
	 * @param criterion
	 *            数量可变的Criterion.
	 * @return
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings("rawtypes")
	protected List findByCriteria(Criterion... criterion)
	{
		return createCriteria(criterion).list();
	}

	/**
	 * Purpose: 根据Criterion条件创建Criteria,后续可进行更多处理,辅助函数.
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:33:47
	 * 
	 * @param criterions
	 * @return
	 * 
	 *         Version: 1.0
	 */
	protected Criteria createCriteria(Criterion... criterions)
	{
		Criteria criteria = getSession().createCriteria(getEntityClass());
		for (Criterion c : criterions)
		{
			criteria.add(c);
		}
		return criteria;
	}

	/**
	 * Purpose: 通过Updater更新对象
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:33:07
	 * 
	 * @param updater
	 * @return
	 * 
	 *         Version: 1.0
	 */
	@SuppressWarnings("unchecked")
	public T updateByUpdater(Updater<T> updater)
	{
		ClassMetadata cm = sessionFactory.getClassMetadata(getEntityClass());
		T bean = updater.getBean();
		T po = (T) getSession().get(getEntityClass(),
				cm.getIdentifier(bean, POJO));
		updaterCopyToPersistentObject(updater, po, cm);
		return po;
	}

	/**
	 * Purpose: 将更新对象拷贝至实体对象，并处理many-to-one的更新。
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:33:22
	 * 
	 * @param updater
	 * @param po
	 * @param cm
	 * 
	 *            Version: 1.0
	 */
	private void updaterCopyToPersistentObject(Updater<T> updater, T po,
			ClassMetadata cm)
	{
		String[] propNames = cm.getPropertyNames();
		String identifierName = cm.getIdentifierPropertyName();
		T bean = updater.getBean();
		Object value;
		for (String propName : propNames)
		{
			if (propName.equals(identifierName))
			{
				continue;
			}
			try
			{
				value = MyBeanUtils.getSimpleProperty(bean, propName);
				if (!updater.isUpdate(propName, value))
				{
					continue;
				}
				cm.setPropertyValue(po, propName, value, POJO);
			} catch (Exception e)
			{
				throw new RuntimeException(
						"copy property to persistent object failed: '"
								+ propName + "'", e);
			}
		}
	}

	/**
	 * Purpose: 获得Dao对于的实体类
	 * 
	 * @author Jie Kim
	 * 
	 *         Create Time: 2014年5月20日 下午4:34:00
	 * 
	 * @return
	 * 
	 *         Version: 1.0
	 */
	abstract protected Class<T> getEntityClass();
}

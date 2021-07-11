package xsightassembler.dao;


import org.hibernate.Session;
import org.hibernate.query.Query;
import xsightassembler.models.UpperSensorModule;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class UpperSensorDao implements UniversalDao {

    public UpperSensorModule findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        UpperSensorModule module = session.get(UpperSensorModule.class, id);
        session.close();
        return module;
    }

    public List<UpperSensorModule> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<UpperSensorModule> query = session.createQuery("Select e from UpperSensorModule e", UpperSensorModule.class);
        List<UpperSensorModule> list = query.getResultList();
        session.close();
        return list;
    }

    public UpperSensorModule findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            UpperSensorModule module = (UpperSensorModule) session.createSQLQuery(
                    "SELECT * FROM public.upper_sensor_module WHERE upper(module) = :sn")
                    .addEntity(UpperSensorModule.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e){
            return null;
        }
    }

    public UpperSensorModule findByInnerModuleSn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<UpperSensorModule> query = builder.createQuery(UpperSensorModule.class);
            Root<UpperSensorModule> root = query.from(UpperSensorModule.class);
            query.select(root).where(builder.or(builder.equal(root.get("module"), sn),
                    builder.equal(root.get("cooler"), sn)));

            Query<UpperSensorModule> q = session.createQuery(query);
            return q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

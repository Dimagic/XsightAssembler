package xsightassembler.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import xsightassembler.models.RadarModule;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class RadarModuleDao implements UniversalDao {

    public RadarModule findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        RadarModule module = session.get(RadarModule.class, id);
        session.close();
        return module;
    }

    public List<RadarModule> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<RadarModule> query = session.createQuery("From RadarModule", RadarModule.class);
        List<RadarModule> list = query.getResultList();
        session.close();
        return list;
    }

    public RadarModule findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            RadarModule module = (RadarModule) session.createSQLQuery(
                    "SELECT * FROM public.radar_module WHERE upper(module) = :sn")
                    .addEntity(RadarModule.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e){
            return null;
        }
    }

    public RadarModule findByInnerModuleSn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<RadarModule> query = builder.createQuery(RadarModule.class);
            Root<RadarModule> root = query.from(RadarModule.class);
            query.select(root).where(builder.or(builder.equal(root.get("module"), sn)));

            Query<RadarModule> q = session.createQuery(query);
            return q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

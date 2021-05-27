package xsightassembler.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import xsightassembler.models.FanModule;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class FanModuleDao implements UniversalDao {
    public FanModule findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        FanModule module = session.get(FanModule.class, id);
        session.close();
        return module;
    }

    public List<FanModule> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<FanModule> query = session.createQuery("From FanModule", FanModule.class);
        List<FanModule> list = query.getResultList();
        session.close();
        return list;
    }

    public FanModule findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            FanModule module = (FanModule) session.createSQLQuery(
                    "SELECT * FROM public.fan_module WHERE upper(module) = :sn")
                    .addEntity(FanModule.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e){
            return null;
        }
    }

    public FanModule findByInnerModuleSn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<FanModule> query = builder.createQuery(FanModule.class);
            Root<FanModule> root = query.from(FanModule.class);
            query.select(root).where(builder.or(builder.equal(root.get("module"), sn)));

            Query<FanModule> q = session.createQuery(query);
            return q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

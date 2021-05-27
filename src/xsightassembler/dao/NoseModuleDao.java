package xsightassembler.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import xsightassembler.models.NoseModule;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class NoseModuleDao implements UniversalDao {

    public NoseModule findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        NoseModule module = session.get(NoseModule.class, id);
        session.close();
        return module;
    }

    public List<NoseModule> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<NoseModule> query = session.createQuery("From NoseModule", NoseModule.class);
        List<NoseModule> list = query.getResultList();
        session.close();
        return list;
    }

    public NoseModule findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            NoseModule module = (NoseModule) session.createSQLQuery(
                    "SELECT * FROM public.nose_module WHERE upper(module) = :sn")
                    .addEntity(NoseModule.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e){
            return null;
        }
    }

    public NoseModule findByInnerModuleSn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<NoseModule> query = builder.createQuery(NoseModule.class);
            Root<NoseModule> root = query.from(NoseModule.class);
            query.select(root).where(builder.or(builder.equal(root.get("module"), sn)));

            Query<NoseModule> q = session.createQuery(query);
            return q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

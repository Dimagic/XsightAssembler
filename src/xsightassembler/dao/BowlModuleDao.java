package xsightassembler.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import xsightassembler.models.BowlModule;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class BowlModuleDao implements UniversalDao {

    public BowlModule findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        BowlModule module = session.get(BowlModule.class, id);
        session.close();
        return module;
    }

    public List<BowlModule> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<BowlModule> query = session.createQuery("From BowlModule", BowlModule.class);
        List<BowlModule> list = query.getResultList();
        session.close();
        return list;
    }

    public BowlModule findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            BowlModule module = (BowlModule) session.createSQLQuery(
                    "SELECT * FROM public.bowl_module WHERE upper(module) = :sn")
                    .addEntity(BowlModule.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e){
            return null;
        }
    }

    public BowlModule findByInnerModuleSn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<BowlModule> query = builder.createQuery(BowlModule.class);
            Root<BowlModule> root = query.from(BowlModule.class);
            query.select(root).where(builder.or(builder.equal(root.get("module"), sn),
                    builder.equal(root.get("comEx"), sn),
                    builder.equal(root.get("breakable"), sn),
                    builder.equal(root.get("carrier"), sn)));

            Query<BowlModule> q = session.createQuery(query);
            return q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

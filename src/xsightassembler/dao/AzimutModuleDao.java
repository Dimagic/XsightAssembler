package xsightassembler.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import xsightassembler.models.AzimutModule;
import xsightassembler.models.BowlModule;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class AzimutModuleDao implements UniversalDao {

    public AzimutModule findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        AzimutModule module = session.get(AzimutModule.class, id);
        session.close();
        return module;
    }

    public List<AzimutModule> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<AzimutModule> query = session.createQuery("From AzimutModule", AzimutModule.class);
        List<AzimutModule> list = query.getResultList();
        session.close();
        return list;
    }

    public AzimutModule findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            AzimutModule module = (AzimutModule) session.createSQLQuery(
                    "SELECT * FROM public.azimut_module WHERE upper(module) = :sn")
                    .addEntity(AzimutModule.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e){
            return null;
        }
    }

    public AzimutModule findByInnerModuleSn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<AzimutModule> query = builder.createQuery(AzimutModule.class);
            Root<AzimutModule>  root = query.from(AzimutModule.class);
            query.select(root).where(builder.or(builder.equal(root.get("module"), sn),
                                                builder.equal(root.get("top"), sn),
                                                builder.equal(root.get("board"), sn)));

            Query<AzimutModule> q = session.createQuery(query);
            return q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

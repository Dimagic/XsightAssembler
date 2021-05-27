package xsightassembler.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import xsightassembler.models.CameraModule;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class CameraModuleDao implements UniversalDao{

    public CameraModule findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        CameraModule module = session.get(CameraModule.class, id);
        session.close();
        return module;
    }

    public List<CameraModule> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<CameraModule> query = session.createQuery("From CameraModule", CameraModule.class);
        List<CameraModule> list = query.getResultList();
        session.close();
        return list;
    }

    public CameraModule findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CameraModule module = (CameraModule) session.createSQLQuery(
                    "SELECT * FROM public.camera_module WHERE upper(module) = :sn")
                    .addEntity(CameraModule.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e){
            return null;
        }
    }

    public CameraModule findByInnerModuleSn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<CameraModule> query = builder.createQuery(CameraModule.class);
            Root<CameraModule> root = query.from(CameraModule.class);
            query.select(root).where(builder.or(builder.equal(root.get("module"), sn),
                    builder.equal(root.get("cameraHouse"), sn),
                    builder.equal(root.get("camera"), sn),
                    builder.equal(root.get("mcu"), sn)));

            Query<CameraModule> q = session.createQuery(query);
            return q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }
}

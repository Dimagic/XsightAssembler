package xsightassembler.dao;

import org.hibernate.Session;
import xsightassembler.models.Isduh;
import xsightassembler.models.Pallet;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

public class IsduhDao implements UniversalDao {

    public Isduh findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Isduh device = session.get(Isduh.class, id);
        session.close();
        return device;
    }

    public List<Isduh> findAll() throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<Isduh> query = session.createQuery("Select e from Isduh e", Isduh.class);
        List<Isduh> list = query.getResultList();
        session.close();
        return list;
    }

    public Isduh findBySn(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            Isduh module = (Isduh) session.createSQLQuery(
                    "SELECT *, upper(sn) FROM public.isduh WHERE sn = :sn")
                    .addEntity(Isduh.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Isduh> getAllBetweenDate(Date start, Date stop) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String q = String.format("select e from Isduh e where" +
                " date between '%s 00:00:00' and '%s 23:59:59' order by date desc", start, stop);
        TypedQuery<Isduh> query = session.createQuery(q, Isduh.class);
        List<Isduh> list = query.getResultList();
        session.close();
        return list;
    }

    public Isduh findByAzimutModule(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            Isduh module = (Isduh) session.createSQLQuery(
                    "SELECT * FROM public.isduh WHERE azimutmodule_id = " +
                            "(SELECT id FROM public.azimut_module WHERE module = :sn)")
                    .addEntity(Isduh.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public Isduh findByUpperSensorModule(String sn) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            Isduh module = (Isduh) session.createSQLQuery(
                    "SELECT * FROM public.isduh WHERE uppersensormodule_id = " +
                            "(SELECT id FROM public.upper_sensor_module WHERE module = :sn)")
                    .addEntity(Isduh.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return module;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public Isduh findByModule(Object val) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            String fieldName = val.getClass().getSimpleName().toLowerCase() + "_id";
            long id = (long) val.getClass().getMethod("getId").invoke(val);
            String q = String.format("SELECT e FROM Isduh e WHERE %s = %s", fieldName, id);
            TypedQuery<Isduh> query = session.createQuery(q, Isduh.class);
            Isduh isduh = query.getSingleResult();
            session.close();
            return isduh;
        } catch (NoResultException e) {
            return null;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CustomException(e);
        }
    }

    public Isduh findByModuleNameAndId(String module, long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            String q = String.format("select e from Isduh e where %s = %s", module, id);
            System.out.println(q);
            TypedQuery<Isduh> query = session.createQuery(q, Isduh.class);
            Isduh isduh = query.getSingleResult();
            session.close();
            return isduh;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public List<Isduh> findByPallet(Pallet pallet) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String q = String.format("select e from Isduh e where pallet_id = %s order by date desc", pallet.getId());
        TypedQuery<Isduh> query = session.createQuery(q, Isduh.class);
        List<Isduh> list = query.getResultList();
        session.close();
        return list;
    }

    public Isduh findByModuleInUpper(String field, long id) throws CustomException {
        try {
            Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
            String q = String.format("select * from public.isduh where " +
                    "uppersensormodule_id = (select id from public.upper_sensor_module where %s = %s)", field, id);
            System.out.println(q);
            Isduh isduh = (Isduh) session.createSQLQuery(q)
                    .addEntity(Isduh.class).getSingleResult();
            session.close();
            return isduh;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }
}

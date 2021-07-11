package xsightassembler.dao.bi;

import org.hibernate.Session;
import org.hibernate.Transaction;
import xsightassembler.dao.UniversalDao;
import xsightassembler.models.BiTest;
import xsightassembler.models.Isduh;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;
import xsightassembler.utils.MsgBox;

import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Date;
import java.util.List;

public class BiTestDao implements UniversalDao {
    public BiTest findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        BiTest biTest = session.get(BiTest.class, id);
        session.close();
        return biTest;
    }

    public List<BiTest> findAll() throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<BiTest> query = session.createQuery("From BiTest", BiTest.class);
        List<BiTest> list = query.getResultList();
        session.close();
        return list;
    }

    public List<BiTest> getRunningTest() throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<BiTest> query = session.createQuery(
                "select e from BiTest e where unplug_date is null", BiTest.class);
        List<BiTest> list = query.getResultList();
        session.close();
        return list;
    }

    public BiTest getRunningTestByLabNum(int labNum) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String q = String.format("select e from BiTest e where lab_num = %s and unplug_date is null", labNum);
        try {
            TypedQuery<BiTest> query = session.createQuery(q, BiTest.class);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            session.close();
        }
    }

    public List<BiTest> getCompleteTestBetweenDates(Date start, Date stop) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String q = String.format("select e from BiTest e where unplug_date " +
                        "between '%s 00:00:00' and '%s 23:59:59' order by unplug_date desc", start, stop);
        TypedQuery<BiTest> query = session.createQuery(q, BiTest.class);
        List<BiTest> list = query.getResultList();
        session.close();
        return list;
    }

    public List<BiTest> getByIsduh(Isduh isduh) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String q = String.format("select e from BiTest e where isduh_id = %s order by start_date", isduh.getId());
        TypedQuery<BiTest> query = session.createQuery(q, BiTest.class);
        List<BiTest> list = query.getResultList();
        session.close();
        return list;
    }

    public BiTest getLastTest(Isduh isduh) throws CustomException {
        try {
            Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
            String q = String.format("select e from BiTest e where e.unplugDate=(select max(e.unplugDate) from BiTest e where e.isduh=%s)", isduh.getId());
            TypedQuery<BiTest> query = session.createQuery(q, BiTest.class);
            BiTest biTest = query.getSingleResult();
            session.close();
            return biTest;
        } catch (NoResultException ignored) {}
        return null;
    }

    public BiTest getLastRunningTest(Isduh isduh) throws CustomException {
        try {
            Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
            String q = String.format("select e from BiTest e where e.unplugDate is null and e.isduh=%s", isduh.getId());
            System.out.println(q);
            Query query = session.createQuery(q);
            BiTest biTest = (BiTest) query.getSingleResult();
            session.close();
            return biTest;
        } catch (NoResultException ignored) {
        } catch (Exception e) {
            throw  new CustomException(e);
        }
        return null;
    }
}

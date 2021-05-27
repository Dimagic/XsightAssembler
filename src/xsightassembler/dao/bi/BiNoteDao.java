package xsightassembler.dao.bi;

import org.hibernate.Session;
import xsightassembler.dao.UniversalDao;
import xsightassembler.models.BiNote;
import xsightassembler.models.BiTest;
import xsightassembler.models.Isduh;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.TypedQuery;
import java.util.List;

public class BiNoteDao implements UniversalDao {

    public BiNote findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        BiNote biNote = session.get(BiNote.class, id);
        session.close();
        return biNote;
    }

    public List<BiNote> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<BiNote> query = session.createQuery("From BiNote", BiNote.class);
        List<BiNote> list = query.getResultList();
        session.close();
        return list;
    }

    public List<BiNote> findByBiTest(BiTest biTest) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String q = String.format("select e from BiNote e where bi_test_id = %s order by note_date", biTest.getId());
        TypedQuery<BiNote> query = session.createQuery(q, BiNote.class);
        List<BiNote> list = query.getResultList();
        session.close();
        return list;
    }

    public List<BiNote> findByIsduh(Isduh isduh) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String q = String.format("select e from BiNote e where bi_test_id in " +
                "(select ee.id from BiTest ee where isduh_id = %s order by plug_date desc)", isduh.getId());
        TypedQuery<BiNote> query = session.createQuery(q, BiNote.class);
        List<BiNote> list = query.getResultList();
        session.close();
        return list;
    }
}

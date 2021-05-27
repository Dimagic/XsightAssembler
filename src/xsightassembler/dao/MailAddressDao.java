package xsightassembler.dao;

import org.hibernate.Session;
import xsightassembler.models.MailAddress;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.TypedQuery;
import java.util.List;

public class MailAddressDao implements UniversalDao {

    public MailAddress findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        MailAddress email = session.get(MailAddress.class, id);
        session.close();
        return email;
    }

    public List<MailAddress> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<MailAddress> query = session.createQuery("From MailAddress", MailAddress.class);
        List<MailAddress> list = query.getResultList();
        session.close();
        return list;
    }
}

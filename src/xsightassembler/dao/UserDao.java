package xsightassembler.dao;

import org.hibernate.Session;
import xsightassembler.models.User;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class UserDao implements UniversalDao {

    public User findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        User user = session.get(User.class, id);
        session.close();
        return user;
    }

    public List<User> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<User> query = session.createQuery("From User", User.class);
        List<User> list = query.getResultList();
        session.close();
        return list;
    }

    public User findByLogin(String login) throws CustomException {
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()) {
            String q = String.format("SELECT * FROM public.users WHERE LOWER(login) = '%s'", login.toLowerCase());
            System.out.println(q);
            User user = (User) session.createSQLQuery(q)
                    .addEntity(User.class).getSingleResult();
            return user;
        } catch (NoResultException e) {
            return null;
        }
    }
}

package xsightassembler.dao;

import org.hibernate.Session;
import xsightassembler.models.History;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

public class HistoryDao implements UniversalDao {
    public History findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        History history = session.get(History.class, id);
        session.close();
        return history;
    }
}

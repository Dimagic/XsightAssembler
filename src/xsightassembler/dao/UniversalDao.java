package xsightassembler.dao;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.simple.JSONObject;
import org.postgresql.util.PSQLException;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Utils;

import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public interface UniversalDao {
    Logger LOGGER = LogManager.getLogger(UniversalDao.class.getName());

    default boolean save(Object obj) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        try {
            session.save(obj);
            tx1.commit();
            session.close();
            return true;
        } catch (Exception e){
            tx1.rollback();
            session.close();
            if (e.getCause() instanceof PSQLException){
                throw new CustomException(e.getCause().getMessage());
            }
            throw new CustomException(e);
        }
    }

    default boolean saveList(List<Object> objectList) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        try {
            for (Object o: objectList){
                session.save(o);
            }
            tx1.commit();
            session.close();
            return true;
        } catch (Exception e){
            tx1.rollback();
            session.close();
            if (e.getCause() instanceof PSQLException){
                throw new CustomException(e.getCause().getMessage());
            }
            throw new CustomException(e);
        }
    }

    default boolean update(Object obj) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        try {
            session.update(obj);
            tx1.commit();
            session.close();
            return true;
        } catch (Exception e){
            tx1.rollback();
            session.close();
            if (e.getCause() instanceof PSQLException){
                throw new CustomException(e.getCause().getMessage());
            }
            throw new CustomException(e);
        }
    }

    default boolean saveOrUpdate(Object obj) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        try {
            session.saveOrUpdate(obj);
            tx1.commit();
            session.close();
            return true;
        } catch (Exception e) {
            tx1.rollback();
            session.close();
            if (e.getCause() instanceof PSQLException){
                throw new CustomException(e.getCause().getMessage());
            }
            throw new CustomException(e);
        }
    }

    default boolean saveOrUpdate(List<Object> objList) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        try {
            for (Object obj: objList) {
                session.saveOrUpdate(obj);
            }
            tx1.commit();
            session.close();
            return true;
        } catch (Exception e) {
            tx1.rollback();
            session.close();
            if (e.getCause() instanceof PSQLException){
                throw new CustomException(e.getCause().getMessage());
            }
            throw new CustomException(e);
        }
    }

    default boolean delete(Object obj) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(obj);
        try {
            tx1.commit();
            session.close();
            return true;
        } catch (PersistenceException hibernateEx) {
            LOGGER.error("Exception", hibernateEx);
            try {
                tx1.rollback();
                MsgBox.msgWarning("Delete operation", "Can't delete this object.\nFor more information see log file");
            } catch (RuntimeException runtimeEx) {
                MsgBox.msgException("Couldn’t Roll Back Transaction", runtimeEx);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return false;
    }

    default Date convertDatePeriod(Date date, int format){
        SimpleDateFormat formatter =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            if (format == 0){
                return formatter.parse(String.format("%s 00:00:00", date));
            } else if (format == 1){
                return formatter.parse(String.format("%s 23:59:59", date));
            } else {
                throw new CustomException("Incorrect format number");
            }
        } catch (ParseException | CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
            return null;
        }
    }

    default HashMap<String, JSONObject> globalSearchString(String s) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        String qString = String.format("with found_rows as (select format('%%I.%%I', table_schema, table_name) " +
                "as table_name,query_to_xml(format('select to_jsonb(t) as table_row from %%I.%%I as t where t::text " +
                "like ''%s'' ', table_schema, table_name), true, false, '') as table_rows from " +
                "information_schema.tables where table_schema = 'public') select table_name, " +
                "x.table_row from found_rows f left join xmltable('//table/row' passing table_rows columns " +
                "table_row text path 'table_row') as x on true", "%%"+s+"%%");
        try {
            TypedQuery<Object> q = session.createSQLQuery(qString);
            List<Object> res = q.getResultList();
            Iterator itr = res.iterator();
            HashMap<String, JSONObject> resMap = new HashMap<>();
            while (itr.hasNext()){
                Object[] obj = (Object[]) itr.next();
                String val = String.valueOf(String.valueOf(obj[1]));
                if (val.equalsIgnoreCase("null")) {
                    continue;
                }
                resMap.put(String.valueOf(String.valueOf(obj[0]))
                        .replace("public.", ""), Utils.jsonToObject(val));
            }
            session.close();
            return resMap;
        } catch (Exception e) {
            throw new CustomException(e);
        }



    }

}

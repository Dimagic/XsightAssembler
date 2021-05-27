package xsightassembler.dao;

import org.hibernate.Session;
import xsightassembler.models.Pallet;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.HibernateSessionFactoryUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class PalletDao implements UniversalDao {

    public Pallet findById(long id) throws CustomException {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Pallet pallet = session.get(Pallet.class, id);
        session.close();
        return pallet;
    }

    public List<Pallet> findAll() throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        TypedQuery<Pallet> query = session.createQuery("From Pallet", Pallet.class);
        List<Pallet> list = query.getResultList();
        session.close();
        return list;
    }

    public Pallet findBySn(String sn) throws CustomException{
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        try {
            Pallet pallet = (Pallet) session.createSQLQuery(
                    "SELECT * FROM public.pallets WHERE upper(pallet_number) = :sn")
                    .addEntity(Pallet.class)
                    .setParameter("sn", sn.trim().toUpperCase()).getSingleResult();
            session.close();
            return pallet;
        } catch (NoResultException e){
            return null;
        }
    }

}

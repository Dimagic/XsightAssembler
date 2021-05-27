package xsightassembler.services;

import xsightassembler.dao.MailAddressDao;
import xsightassembler.models.MailAddress;
import xsightassembler.utils.CustomException;

import java.util.List;

public class MailAddressService {
    private final MailAddressDao dao = new MailAddressDao();

    public MailAddress findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<MailAddress> findAll() throws CustomException {
        return dao.findAll();
    }

    public boolean save(MailAddress email) throws CustomException {
        return dao.save(email);
    }

    public boolean saveOrUpdate(MailAddress email) throws CustomException {
        return dao.saveOrUpdate(email);
    }

    public boolean delete(MailAddress email) throws CustomException {
        return dao.delete(email);
    }

    public  boolean update(MailAddress email) throws  CustomException {
        return dao.update(email);
    }
}

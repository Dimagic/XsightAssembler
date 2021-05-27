package xsightassembler.services;

import org.json.simple.JSONObject;
import xsightassembler.dao.IsduhDao;
import xsightassembler.models.Isduh;
import xsightassembler.models.Pallet;
import xsightassembler.utils.CustomException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class IsduhService {

    private final IsduhDao dao = new IsduhDao();

    public Isduh findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<Isduh> findAll() throws CustomException {
        return dao.findAll();
    }

    public boolean save(Object module) throws CustomException {
        return dao.save(module);
    }

    public boolean update(Object module) throws CustomException {
        return dao.update(module);
    }

    public boolean delete(Object module) throws CustomException {
        return dao.delete(module);
    }

    public boolean saveOrUpdate(Object module) throws CustomException {
        return dao.saveOrUpdate(module);
    }

    public Isduh findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public Isduh findByAzimutModule(String sn) throws CustomException {
        return dao.findByAzimutModule(sn);
    }

    public List<Isduh> getAllBetweenDate(Date start, Date stop) throws CustomException {
        return dao.getAllBetweenDate(start, stop);
    }

    public Isduh findByModule(Object val) throws CustomException {
        return dao.findByModule(val);
    }

    public List<Isduh> findByPallet(Pallet pallet) throws CustomException {
        return dao.findByPallet(pallet);
    }

    public HashMap<String, JSONObject> globalSearchString(String s) throws CustomException {
        return dao.globalSearchString(s);
    }

    public Isduh findByModuleNameAndId(String module, long id) throws CustomException {
        return dao.findByModuleNameAndId(module, id);
    }

    public Isduh findByModuleInUpper(String field, long id) throws CustomException {
        return dao.findByModuleInUpper(field, id);
    }
}

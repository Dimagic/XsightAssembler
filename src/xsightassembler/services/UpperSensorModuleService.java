package xsightassembler.services;

import org.json.simple.JSONObject;
import xsightassembler.dao.UpperSensorDao;
import xsightassembler.models.UpperSensorModule;
import xsightassembler.utils.CustomException;

import java.util.HashMap;
import java.util.List;

public class UpperSensorModuleService {

    private final UpperSensorDao dao = new UpperSensorDao();

    public UpperSensorModule findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<UpperSensorModule> findAll() throws CustomException {
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

    public UpperSensorModule findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public UpperSensorModule findByInnerModuleSn(String sn) throws CustomException {
        return dao.findByInnerModuleSn(sn);
    }

    public HashMap<String, JSONObject> globalSearchString(String s) throws CustomException {
        return dao.globalSearchString(s);
    }
}

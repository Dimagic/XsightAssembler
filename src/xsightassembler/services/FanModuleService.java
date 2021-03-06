package xsightassembler.services;

import xsightassembler.dao.FanModuleDao;
import xsightassembler.models.FanModule;
import xsightassembler.utils.CustomException;

import java.util.List;

public class FanModuleService {

    private final FanModuleDao dao = new FanModuleDao();

    public FanModuleService() {
    }

    public FanModule findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<FanModule> findAll() throws CustomException {
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

    public FanModule findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public FanModule findByInnerModuleSn(String sn) throws CustomException {
        return dao.findByInnerModuleSn(sn);
    }
}

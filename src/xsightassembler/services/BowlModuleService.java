package xsightassembler.services;

import xsightassembler.dao.BowlModuleDao;
import xsightassembler.models.BowlModule;
import xsightassembler.utils.CustomException;

import java.util.List;

public class BowlModuleService {
    private final BowlModuleDao dao = new BowlModuleDao();

    public BowlModuleService() {
    }

    public BowlModule findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<BowlModule> findAll() throws CustomException {
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

    public BowlModule findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public BowlModule findByInnerModuleSn(String sn) throws CustomException {
        return dao.findByInnerModuleSn(sn);
    }
}

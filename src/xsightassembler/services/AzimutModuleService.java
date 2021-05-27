package xsightassembler.services;

import xsightassembler.dao.AzimutModuleDao;
import xsightassembler.models.AzimutModule;
import xsightassembler.utils.CustomException;

import java.util.List;

public class AzimutModuleService {
    private final AzimutModuleDao dao = new AzimutModuleDao();

    public AzimutModuleService() {
    }

    public AzimutModule findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<AzimutModule> findAll() throws CustomException {
        return dao.findAll();
    }

    public boolean save(Object module) throws CustomException {
        return dao.save(module);
    }

    public boolean delete(Object module) throws CustomException {
        return dao.delete(module);
    }

    public boolean saveOrUpdate(Object module) throws CustomException {
        return dao.saveOrUpdate(module);
    }

    public AzimutModule findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public AzimutModule findByInnerModuleSn(String sn) throws CustomException {
        return dao.findByInnerModuleSn(sn);
    }
}

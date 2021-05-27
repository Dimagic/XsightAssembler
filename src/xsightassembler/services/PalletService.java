package xsightassembler.services;

import xsightassembler.dao.PalletDao;
import xsightassembler.models.Pallet;
import xsightassembler.utils.CustomException;

import java.util.List;

public class PalletService {
    private final PalletDao dao = new PalletDao();

    public PalletService() {
    }

    public Pallet findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<Pallet> findAll() throws CustomException {
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

    public Pallet findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }
}

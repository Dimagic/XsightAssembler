package xsightassembler.services;

import xsightassembler.dao.HistoryDao;
import xsightassembler.models.History;
import xsightassembler.utils.CustomException;

public class HistoryService {
    private final HistoryDao dao = new HistoryDao();

    public HistoryService() {
    }

    public History findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public boolean save(Object module) throws CustomException {
        System.out.println(module);
        return dao.save(module);
    }

    public boolean saveOrUpdate(Object module) throws CustomException {
        return dao.saveOrUpdate(module);
    }
}

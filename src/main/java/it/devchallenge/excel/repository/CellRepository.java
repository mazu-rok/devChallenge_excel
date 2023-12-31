package it.devchallenge.excel.repository;

import it.devchallenge.excel.model.CellEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CellRepository extends MongoRepository<CellEntity, UUID> {
    Optional<CellEntity> findBySheetNameIgnoreCaseAndNameIgnoreCase(String sheetName, String cellName);
    List<CellEntity> findAllBySheetNameIgnoreCase(String sheetName);

    @Query(value = "{'value': { $regex: '^=.*(" + "?1" + ").*', $options: 'i' }, 'sheetName': {'$eq': '?0'}}")
    List<CellEntity> findByValueContainingInput(String sheetName, String input);
    long countByValueIgnoreCase(String value);
}

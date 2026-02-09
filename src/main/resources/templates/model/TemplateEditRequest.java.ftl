package ${packageName}.model.dto.${dataKey};

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ${upperDataKey}EditRequest implements Serializable {

    /**
     * id
     */
    private Long id;
    private static final long serialVersionUID = 1L;
}
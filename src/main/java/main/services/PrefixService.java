package main.services;

import main.model.repository.PrefixRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PrefixService {

    @Autowired
    private PrefixRepository prefixRepository;


    public void repo() {

//        prefixRepository.addPrefix();

    }


}


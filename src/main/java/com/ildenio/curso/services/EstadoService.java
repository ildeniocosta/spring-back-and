package com.ildenio.curso.services;

import com.ildenio.curso.domain.Estado;
import com.ildenio.curso.repositories.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadoService {
    @Autowired
    private EstadoRepository repo;
    public List<Estado>findAll(){
        return repo.findAllByOrderByNome();
    }
}

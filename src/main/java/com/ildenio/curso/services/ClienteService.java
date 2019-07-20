package com.ildenio.curso.services;

import com.ildenio.curso.domain.Cidade;
import com.ildenio.curso.domain.Cliente;
import com.ildenio.curso.domain.Endereco;
import com.ildenio.curso.domain.enums.Perfil;
import com.ildenio.curso.domain.enums.TipoCliente;
import com.ildenio.curso.dto.ClienteDTO;
import com.ildenio.curso.dto.ClienteNewDTO;
import com.ildenio.curso.repositories.CidadeRepository;
import com.ildenio.curso.repositories.ClienteRepository;
import com.ildenio.curso.repositories.EnderecoRepository;
import com.ildenio.curso.security.UserSS;
import com.ildenio.curso.services.exception.AuthorizationException;
import com.ildenio.curso.services.exception.DataIntegrityException;
import com.ildenio.curso.services.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;



@Service
public class ClienteService {
    @Autowired
    private ClienteRepository repo;
    @Autowired
    private CidadeRepository cidadeRepository;
    @Autowired
    private EnderecoRepository enderecoRepository;
    @Autowired
    private BCryptPasswordEncoder pe;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private ImageService imageService;
    @Value("${img.profile.size}")
    private Integer size;

    @Value("${img.prefix.client.profile}")
    private String prefix;

    @Transactional
    public Cliente insert(Cliente obj) {
        obj.setId(null);
        obj = repo.save(obj);
        enderecoRepository.saveAll(obj.getEnderecos());
        return obj;
    }
    public Cliente find(Integer id) {
        UserSS user = UserService.authenticated();
        if (user==null || !user.hasRole(Perfil.ADMIN) && !id.equals(user.getId())) {
            throw new AuthorizationException("Acesso negado");
        }
        Optional<Cliente> obj = repo.findById(id);
        return obj.orElseThrow(()-> new ObjectNotFoundException("Objeto não encontrado! id: "+id+" ,Tipo: "+Cliente.class.getName()));
    }
    public Cliente update(Cliente obj){
        Cliente newObj = find(obj.getId());
       updateData(newObj,obj);
        return repo.save(newObj);
    }
    public void delete(Integer id){
      find(id);
        try{
            repo.deleteById(id);
        }
        catch(DataIntegrityViolationException e){
            throw new DataIntegrityException("Não é possível excluir um clieten porque há pedidos relacionadas");
        }

    }
    public List<Cliente> findAll() {
        return repo.findAll();
    }
    public Page<Cliente> findPage(Integer page, Integer linesPerPage, String orderBy, String direction) {
        PageRequest pageRequest = PageRequest.of(page, linesPerPage,Direction.valueOf(direction), orderBy);
        return repo.findAll(pageRequest);
    }
    public Cliente fromDto(ClienteDTO objDto){
       return new Cliente(objDto.getId(),objDto.getNome(),objDto.getEmail(),null,null,null);
    }
    public Cliente fromDto(ClienteNewDTO objDto) {
        Cliente cli = new Cliente(null, objDto.getNome(), objDto.getEmail(), objDto.getCpfOuCnpj(),TipoCliente.toEnum(objDto.getTipo()),pe.encode(objDto.getSenha()));
        Cidade cid = new Cidade(objDto.getCidadeId(),null,null);
        Endereco end = new Endereco(null, objDto.getLogradouro(), objDto.getNumero(), objDto.getComplemento(), objDto.getBairro(), objDto.getCep(), cli, cid);
        cli.getEnderecos().add(end);
        cli.getTelefones().add(objDto.getTelefone1());
        if (objDto.getTelefone2() != null) {
            cli.getTelefones().add(objDto.getTelefone2());
        }
        if (objDto.getTelefone3() != null) {
            cli.getTelefones().add(objDto.getTelefone3());
        }
        return cli;
    }
    private void updateData(Cliente newObj,Cliente obj){
        newObj.setNome(obj.getNome());
        newObj.setEmail(obj.getEmail());
    }
    public URI uploadProfilePicture(MultipartFile multipartFile) {
        UserSS user = UserService.authenticated();
        if (user == null) {
            throw new AuthorizationException("Acesso negado");
        }

        BufferedImage jpgImage = imageService.getJpgImageFromFile(multipartFile);
        jpgImage = imageService.cropSquare(jpgImage);
        jpgImage = imageService.resize(jpgImage, size);
        String fileName = prefix + user.getId() + ".jpg";

        return s3Service.uploadFile(imageService.getInputStream(jpgImage, "jpg"), fileName, "image");
    }
    public Cliente findByEmail(String email) {
        UserSS user = UserService.authenticated();
        if (user == null || !user.hasRole(Perfil.ADMIN) && !email.equals(user.getUsername())) {
            throw new AuthorizationException("Acesso negado");
        }

        Cliente obj = repo.findByEmail(email);
        if (obj == null) {
            throw new ObjectNotFoundException(
                    "Objeto não encontrado! Id: " + user.getId() + ", Tipo: " + Cliente.class.getName());
        }
        return obj;
    }
}


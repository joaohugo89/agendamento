package com.agendamento.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.catalog.dto.BlockRequest;
import com.agendamento.catalog.dto.BlockResponse;
import com.agendamento.common.BusinessRuleException;
import com.agendamento.common.NotFoundException;

@Service
public class BlockService {

    private final BlockRepository repository;
    private final ProfessionalRepository professionalRepository;

    public BlockService(BlockRepository repository, ProfessionalRepository professionalRepository) {
        this.repository = repository;
        this.professionalRepository = professionalRepository;
    }

    @Transactional(readOnly = true)
    public List<BlockResponse> list(UUID businessId) {
        Map<UUID, String> names = new HashMap<>();
        professionalRepository.findAllWithServices(businessId)
                .forEach(professional -> names.put(professional.getId(), professional.getName()));
        return repository.findByBusinessIdOrderByStartsAtDesc(businessId).stream()
                .map(block -> BlockResponse.from(block, names.get(block.getProfessionalId())))
                .toList();
    }

    @Transactional
    public BlockResponse create(UUID businessId, BlockRequest request) {
        validate(businessId, request);
        Block block = new Block(businessId, request.professionalId(), request.startsAt(),
                request.endsAt(), request.reason());
        return BlockResponse.from(repository.save(block), professionalName(businessId, block.getProfessionalId()));
    }

    @Transactional
    public BlockResponse update(UUID businessId, UUID id, BlockRequest request) {
        validate(businessId, request);
        Block block = repository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Bloqueio nao encontrado."));
        block.update(request.professionalId(), request.startsAt(), request.endsAt(), request.reason());
        return BlockResponse.from(repository.save(block), professionalName(businessId, block.getProfessionalId()));
    }

    @Transactional
    public void delete(UUID businessId, UUID id) {
        Block block = repository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Bloqueio nao encontrado."));
        repository.delete(block);
    }

    private void validate(UUID businessId, BlockRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new BusinessRuleException("O inicio do bloqueio deve ser antes do fim.");
        }
        if (request.professionalId() != null) {
            professionalRepository.findByIdAndBusinessId(request.professionalId(), businessId)
                    .orElseThrow(() -> new NotFoundException("Profissional nao encontrado."));
        }
    }

    private String professionalName(UUID businessId, UUID professionalId) {
        if (professionalId == null) {
            return null;
        }
        return professionalRepository.findByIdAndBusinessId(professionalId, businessId)
                .map(Professional::getName)
                .orElse(null);
    }
}
